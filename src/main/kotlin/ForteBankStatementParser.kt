import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object ForteBankStatementParser {
    private val logger = LoggerFactory.getLogger(ForteBankStatementParser::class.java)

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun parse(pdfFile: File): BankStatement {
        val document: PDDocument = Loader.loadPDF(pdfFile)
        val stripper = PDFTextStripper()
        val text = stripper.getText(document)
        document.close()

        logger.debug("Extracted text from PDF:\n$text")

        return parseBankStatement(text)
    }

    fun parse(pdfBytes: ByteArray): BankStatement {
        val document: PDDocument = Loader.loadPDF(pdfBytes)
        val stripper = PDFTextStripper()
        val text = stripper.getText(document)
        document.close()

        logger.debug("Extracted text from PDF:\n$text")

        return parseBankStatement(text)
    }

    private fun parseBankStatement(text: String): BankStatement {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        logger.info("Starting PDF parsing: ${lines.size} lines extracted")

        // Extract account holder - looking for IIN line
        val accountHolder = extractAccountHolder(lines)
        logger.debug("Extracted account holder: $accountHolder")

        // Extract account number
        val accountNumber = extractAccountNumber(lines)
        logger.debug("Extracted account number: $accountNumber")

        // Extract currency
        val currency = extractCurrency(lines)
        logger.debug("Extracted currency: $currency")

        // Extract period
        val period = extractPeriod(lines)
        logger.debug("Extracted period: ${period.from} - ${period.to}")

        // Extract opening and closing balance
        val (openingBalance, closingBalance) = extractBalances(lines)
        logger.debug("Extracted balances: opening=$openingBalance, closing=$closingBalance")

        logger.info("Metadata extraction complete. Starting transaction parsing...")

        // Extract transactions
        val transactions = extractTransactions(lines, currency)

        logger.info("PDF parsing complete: ${transactions.size} transactions extracted")

        return BankStatement(
            accountHolder = accountHolder,
            accountNumber = accountNumber,
            currency = currency,
            period = period,
            openingBalance = openingBalance,
            closingBalance = closingBalance,
            transactions = transactions
        )
    }

    private fun extractAccountHolder(lines: List<String>): String {
        // Look for IIN line, account holder is usually before it
        for (i in lines.indices) {
            if (lines[i].startsWith("IIN:")) {
                if (i > 0) {
                    return lines[i - 1]
                }
            }
        }
        return "Unknown"
    }

    private fun extractAccountNumber(lines: List<String>): String {
        // Pattern 1: "Account number: KZ1896502F0018918306"
        val pattern1 = Regex("Account number:\\s*([A-Z0-9]+)")
        // Pattern 2: "№ KZ5496503F0011445795 (EUR)" or "№ KZ5496503F0011445795"
        val pattern2 = Regex("№\\s*([A-Z0-9]+)")

        for (line in lines) {
            pattern1.find(line)?.let { match ->
                if (match.groupValues.size > 1) {
                    return match.groupValues[1].trim()
                }
            }
            pattern2.find(line)?.let { match ->
                if (match.groupValues.size > 1) {
                    return match.groupValues[1].trim()
                }
            }
        }
        return "Unknown"
    }

    private fun extractCurrency(lines: List<String>): String {
        // Look for "Account currency:" pattern
        val currencyPattern = Regex("Account currency:\\s*([A-Z]{3})")
        for (line in lines) {
            val match = currencyPattern.find(line)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }
        }
        return "USD"
    }

    private fun extractPeriod(lines: List<String>): DatePeriod {
        // Look for "For the period: from DD.MM.YYYY to DD.MM.YYYY"
        val periodPattern = Regex("For the period:\\s*from\\s*(\\d{2}\\.\\d{2}\\.\\d{4})\\s*to\\s*(\\d{2}\\.\\d{2}\\.\\d{4})")
        for (line in lines) {
            val match = periodPattern.find(line)
            if (match != null && match.groupValues.size > 2) {
                val from = LocalDate.parse(match.groupValues[1], dateFormatter)
                val to = LocalDate.parse(match.groupValues[2], dateFormatter)
                return DatePeriod(from, to)
            }
        }
        return DatePeriod(LocalDate.now().minusMonths(1), LocalDate.now())
    }

    private fun extractBalances(lines: List<String>): Pair<Double, Double> {
        var closingBalance = 0.0

        // Look for "Available as of DD.MM.YYYY: XXX.XX USD"
        val availablePattern = Regex("Available as of [\\d.]+:\\s*([\\d,]+\\.\\d{2})\\s+[A-Z]{3}")
        for (line in lines) {
            val match = availablePattern.find(line)
            if (match != null && match.groupValues.size > 1) {
                closingBalance = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
                break
            }
        }

        // For card statements, we don't always have opening balance in the same format
        // We'll calculate it from transactions if needed
        val openingBalance = 0.0

        return Pair(openingBalance, closingBalance)
    }

    private fun extractTransactions(lines: List<String>, accountCurrency: String): List<Transaction> {
        val transactions = mutableListOf<Transaction>()

        // Find where transactions start
        var inTransactionSection = false
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            if (line.contains("Date Sum Description Details") ||
                line.contains("Debit card statement details")) {
                inTransactionSection = true
                i++
                continue
            }

            if (!inTransactionSection) {
                i++
                continue
            }

            // Skip "Date Sum Description Details" header line that repeats
            if (line.trim() == "Date Sum Description Details") {
                i++
                continue
            }

            // Try to match a date at the start of the line
            val dateMatch = Regex("^(\\d{2}\\.\\d{2}\\.\\d{4})").find(line)

            if (dateMatch != null) {
                // This is a transaction line - collect all lines for this transaction
                val date = try {
                    LocalDate.parse(dateMatch.groupValues[1], dateFormatter)
                } catch (e: DateTimeParseException) {
                    logger.warn("Failed to parse date: ${dateMatch.groupValues[1]}", e)
                    i++
                    continue
                }

                // Collect all lines for this transaction
                val txLines = mutableListOf(line)
                var j = i + 1
                while (j < lines.size) {
                    val nextLine = lines[j]
                    if (nextLine.matches(Regex("^\\d{2}\\.\\d{2}\\.\\d{4}.*")) ||
                        nextLine.startsWith("Page ") ||
                        nextLine.trim() == "Date Sum Description Details") {
                        break
                    }
                    if (nextLine.trim().isNotEmpty()) {
                        txLines.add(nextLine.trim())
                    }
                    j++
                }

                // Join all lines for parsing with smart word-break handling
                val fullText = smartJoinLines(txLines)

                // Extract amount in account currency: "-3.26 USD"
                val accountAmountMatch = Regex("([-]?\\d+\\.\\d{2})\\s+([A-Z]{3})").find(fullText)
                if (accountAmountMatch == null) {
                    logger.warn("Failed to parse amount from transaction: $fullText")
                    i = j
                    continue
                }

                val amount = accountAmountMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                val currency = accountAmountMatch.groupValues[2]

                // Extract transaction amount if present: "(59.40 MYR)"
                var transactionAmount: Double? = null
                var transactionCurrency: String? = null
                val txAmountMatch = Regex("\\((\\d+\\.\\d{2})\\s+([A-Z]{3})\\)").find(fullText)
                if (txAmountMatch != null) {
                    transactionAmount = txAmountMatch.groupValues[1].toDoubleOrNull()
                    transactionCurrency = txAmountMatch.groupValues[2]
                }

                // Extract transaction type and filter - only save card purchases
                val type = extractTransactionType(fullText)
                if (type != "Purchase" && type != "Purchase with bonuses") {
                    // Skip non-purchase transactions (transfers, refunds, withdrawals, etc.)
                    i = j
                    continue
                }

                // Parse merchant details (everything after type)
                // Remove date, amounts, and type prefix to get clean merchant details
                var merchantDetails = fullText

                // Remove date prefix: "16.10.2025 " or "07.06.2025-" or "16.07.20238.74" (separator can be missing)
                merchantDetails = merchantDetails.replaceFirst(Regex("^\\d{2}\\.\\d{2}\\.\\d{4}[-\\s]*"), "")

                // Remove account amount: "-70.80 USD " or "1.89 USD " or "70.80 USD"
                merchantDetails = merchantDetails.replaceFirst(Regex("^[-]?\\d+\\.\\d{2}\\s+[A-Z]{3}\\s*"), "")

                // Remove transaction amount if present: "(299.00 MYR) " or "(7.80 MYR)"
                merchantDetails = merchantDetails.replaceFirst(Regex("^\\([\\d.]+\\s+[A-Z]{3}\\)\\s*"), "")

                // Remove transaction type prefix (space after type is optional)
                val typePattern = "(Purchase with bonuses|Purchase|Transfer|Refund|Account replenishment|Cash withdrawal|Fee)\\s*"
                merchantDetails = merchantDetails.replaceFirst(Regex("^$typePattern"), "")

                val (merchantName, merchantLocation, mccCode, bankName, paymentMethod) = parseDetails(merchantDetails)

                logger.debug("Parsed merchant details from '$merchantDetails': name='$merchantName', location='$merchantLocation'")

                transactions.add(
                    Transaction(
                        date = date,
                        amount = amount,
                        currency = currency,
                        transactionAmount = transactionAmount,
                        transactionCurrency = transactionCurrency,
                        merchantName = merchantName,
                        merchantLocation = merchantLocation,
                        mccCode = mccCode,
                        bankName = bankName,
                        paymentMethod = paymentMethod,
                        description = fullText
                    )
                )

                // Log progress every 50 transactions
                if (transactions.size % 50 == 0) {
                    logger.info("Transaction parsing progress: ${transactions.size} transactions parsed...")
                }

                i = j
            } else {
                i++
            }
        }

        return transactions
    }

    private fun extractTransactionType(line: String): String {
        return when {
            line.contains("Purchase with bonuses") -> "Purchase with bonuses"
            line.contains("Purchase") -> "Purchase"
            line.contains("Transfer") -> "Transfer"
            line.contains("Refund") -> "Refund"
            line.contains("Account replenishment") -> "Account replenishment"
            line.contains("Cash withdrawal") -> "Cash withdrawal"
            else -> "Other"
        }
    }

    private data class TransactionDetails(
        val merchantName: String?,
        val merchantLocation: String?,
        val mccCode: String?,
        val bankName: String?,
        val paymentMethod: String?
    )

    private fun parseDetails(details: String): TransactionDetails {
        if (details.isBlank()) {
            return TransactionDetails(null, null, null, null, null)
        }

        // Extract MCC code: "MCC: 5411"
        var mccCode: String? = null
        val mccMatch = Regex("MCC:\\s*(\\d+)").find(details)
        if (mccMatch != null) {
            mccCode = mccMatch.groupValues[1]
        }

        // Extract payment method (usually at the end): "APPLE PAY" or card number
        var paymentMethod: String? = null
        if (details.contains("APPLE PAY")) {
            paymentMethod = "APPLE PAY"
        } else if (details.contains("GOOGLE PAY")) {
            paymentMethod = "GOOGLE PAY"
        }

        // Extract bank name: text before "MCC:" or after last comma
        var bankName: String? = null
        val bankPattern = Regex(",\\s*([^,]+),\\s*MCC:")
        val bankMatch = bankPattern.find(details)
        if (bankMatch != null) {
            bankName = bankMatch.groupValues[1].trim()
            // Normalize whitespace and check if it's "Bank not specified" or "Банк не определён"
            // PDF extraction sometimes adds extra spaces: "Bank not specifi ed", "Bank not spec ified", etc.
            // Remove ALL spaces (including within words) before comparing
            val normalizedBankName = bankName.replace(Regex("\\s+"), "").lowercase()
            if (normalizedBankName == "banknotspecified" || normalizedBankName == "банкнеопределён") {
                bankName = null
            }
        }

        // Extract merchant name and location
        // Format 1 (with commas): "NSK GROCER- QCM,QUILL CITY MALL,KUALA LUMPUR,MY"
        // Format 2 (space-separated): "WWW.BUSTICKET4.ME PODGORICA ME"
        var merchantName: String? = null
        var merchantLocation: String? = null

        if (details.contains(",")) {
            // Format 1: Split by commas
            val parts = details.split(",").map { it.trim() }
            if (parts.isNotEmpty()) {
                // First part is usually merchant name
                merchantName = parts[0]

                // Try to find location parts (before bank name)
                val locationParts = mutableListOf<String>()
                for (part in parts.drop(1)) {
                    if (part.contains("MCC:") || part.contains("Bank") ||
                        part == "APPLE PAY" || part == "GOOGLE PAY") {
                        break
                    }
                    locationParts.add(part)
                }
                if (locationParts.isNotEmpty()) {
                    merchantLocation = locationParts.joinToString(", ")
                }
            }
        } else {
            // Format 2: Space-separated, likely "MERCHANT NAME PARTS CITY PARTS COUNTRY_CODE"
            // Strategy: Last word = country code (2 letters), everything else goes to location
            // Let LocationParsingService handle city extraction later
            //
            // Examples:
            // - "CAFFE BAR CASPER BUDVA ME" -> merchant="CAFFE BAR CASPER", location="BUDVA ME"
            // - "CIRCLE K HO CHI MINH VN" -> merchant="CIRCLE K", location="HO CHI MINH VN"
            // - "7-ELEVEN KUALA LUMPUR MY" -> merchant="7-ELEVEN", location="KUALA LUMPUR MY"

            // Remove known suffixes (MCC, bank, payment method) first
            var cleanDetails = details
            cleanDetails = cleanDetails.replaceFirst(Regex("\\s*MCC:.*$"), "")
            cleanDetails = cleanDetails.replaceFirst(Regex("\\s*APPLE PAY.*$"), "")
            cleanDetails = cleanDetails.replaceFirst(Regex("\\s*GOOGLE PAY.*$"), "")

            val words = cleanDetails.split("\\s+".toRegex()).filter { it.isNotBlank() }

            if (words.size >= 2) {
                // Check if last word is 2-letter country code
                val lastWord = words.last()
                val isCountryCode = lastWord.length == 2 && lastWord.all { it.isLetter() }

                if (isCountryCode) {
                    // Simple strategy: first word = merchant, rest = location (city + country)
                    // This handles both single and multi-word cities automatically
                    // LocationParsingService will extract city from this later

                    merchantName = words.first()
                    merchantLocation = words.drop(1).joinToString(" ")
                } else {
                    // No country code - treat first word as merchant, rest as location
                    merchantName = words.firstOrNull()
                    merchantLocation = words.drop(1).joinToString(" ").ifBlank { null }
                }
            } else if (words.size == 1) {
                // Only one word - could be merchant or country
                merchantName = words[0]
                merchantLocation = null
            }
        }

        return TransactionDetails(merchantName, merchantLocation, mccCode, bankName, paymentMethod)
    }

    /**
     * Joins multiple lines with smart spacing based on capitalization.
     *
     * Rules:
     * - Lowercase letter at start: join without space (word continuation)
     * - Uppercase letter at start:
     *   - If looks like abbreviation continuation (single uppercase or all uppercase): no space
     *   - Otherwise: add space
     *
     * Examples:
     * - ["First", "Abu Dhabi Bank"] -> "First Abu Dhabi Bank"
     * - ["Freedom Bank Ka", "zakhstan"] -> "Freedom Bank Kazakhstan"
     * - ["Thai Smart Card C", "ompany"] -> "Thai Smart Card Company"
     * - ["BC", "C"] -> "BCC" (abbreviation)
     * - ["Unlimint E", "U Ltd"] -> "Unlimint EU Ltd" (abbreviation)
     */
    private fun smartJoinLines(lines: List<String>): String {
        if (lines.isEmpty()) return ""
        if (lines.size == 1) return lines[0]

        val result = StringBuilder()

        for (i in lines.indices) {
            if (i == 0) {
                result.append(lines[i])
                continue
            }

            val prevLine = lines[i - 1]
            val currLine = lines[i]
            val firstChar = currLine.firstOrNull()

            when {
                // Lowercase or non-letter at start: join without space (word continuation)
                firstChar?.isLowerCase() == true || !firstChar?.isLetter()!! -> {
                    result.append(currLine)
                }
                // Current line looks like abbreviation part (only uppercase letters, possibly 1-2 chars)
                // AND previous line ends with uppercase letter
                // Example: "BC" + "C" -> "BCC", "E" + "U" -> "EU"
                isAbbreviationContinuation(prevLine, currLine) -> {
                    result.append(currLine)
                }
                // Otherwise: uppercase at start, add space
                else -> {
                    result.append(" ").append(currLine)
                }
            }
        }

        return result.toString()
            .replace(Regex("-\\s+"), "-")  // Handle hyphenated breaks: "SUPER- MARKET" -> "SUPER-MARKET"
            .replace(Regex("\\s+"), " ")    // Normalize multiple spaces to single space
            .trim()
    }

    /**
     * Check if current line is a continuation of an abbreviation.
     *
     * Examples:
     * - prevLine="BC", currLine="C" -> true (BCC)
     * - prevLine="Unlimint E", currLine="U Ltd" -> true (EU before Ltd)
     * - prevLine="First", currLine="Abu Dhabi" -> false (new word)
     */
    private fun isAbbreviationContinuation(prevLine: String, currLine: String): Boolean {
        // Previous line must end with uppercase letter
        val prevEndsWithUpper = prevLine.lastOrNull()?.isUpperCase() == true
        if (!prevEndsWithUpper) return false

        // Current line should be 1-2 uppercase letters, possibly followed by space and more text
        // Examples: "C", "U Ltd", "EU"
        val startsWithAbbreviation = Regex("^[A-Z]{1,2}(\\s|$)").find(currLine) != null

        return startsWithAbbreviation
    }
}
