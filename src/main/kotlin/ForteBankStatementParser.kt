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

        // Extract account holder - looking for IIN line
        val accountHolder = extractAccountHolder(lines)

        // Extract account number
        val accountNumber = extractAccountNumber(lines)

        // Extract currency
        val currency = extractCurrency(lines)

        // Extract period
        val period = extractPeriod(lines)

        // Extract opening and closing balance
        val (openingBalance, closingBalance) = extractBalances(lines)

        // Extract transactions
        val transactions = extractTransactions(lines, currency)

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
        // Look for "Account number:" pattern
        val accountPattern = Regex("Account number:\\s*([A-Z0-9]+)")
        for (line in lines) {
            val match = accountPattern.find(line)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
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

    private fun extractTransactions(lines: List<String>, currency: String): List<Transaction> {
        val transactions = mutableListOf<Transaction>()

        // Find where transactions start (after "Date Sum Description Details")
        var inTransactionSection = false
        var currentDate: LocalDate? = null
        var currentAmount: Double? = null
        var currentCurrency: String? = null
        var description = StringBuilder()

        for (line in lines) {
            if (line.contains("Date Sum Description Details") ||
                line.contains("Debit card statement details")) {
                inTransactionSection = true
                continue
            }

            if (!inTransactionSection) continue

            // Try to match a date at the start of the line
            val dateMatch = Regex("^(\\d{2}\\.\\d{2}\\.\\d{4})").find(line)

            if (dateMatch != null) {
                // Save previous transaction if exists
                if (currentDate != null && currentAmount != null) {
                    val txDateTime = currentDate.atStartOfDay()
                    val debit = if (currentAmount < 0) -currentAmount else null
                    val credit = if (currentAmount > 0) currentAmount else null

                    transactions.add(
                        Transaction(
                            dateTime = txDateTime,
                            description = description.toString().trim(),
                            debit = debit,
                            credit = credit,
                            balance = 0.0,
                            reference = null
                        )
                    )
                }

                // Start new transaction
                currentDate = try {
                    LocalDate.parse(dateMatch.groupValues[1], dateFormatter)
                } catch (e: DateTimeParseException) {
                    logger.warn("Failed to parse date: ${dateMatch.groupValues[1]}", e)
                    null
                }

                // Extract amount - look for pattern like "-3.26 USD" or "0.69 USD"
                val amountMatch = Regex("[-]?\\d+\\.\\d{2}\\s+[A-Z]{3}").find(line)
                if (amountMatch != null) {
                    val amountStr = amountMatch.value.split("\\s+".toRegex())[0]
                    currentAmount = amountStr.toDoubleOrNull()
                    currentCurrency = amountMatch.value.split("\\s+".toRegex()).getOrNull(1)
                }

                // Extract description - everything after the amount
                val descMatch = Regex("[-]?\\d+\\.\\d{2}\\s+[A-Z]{3}\\s+(.+)").find(line)
                if (descMatch != null && descMatch.groupValues.size > 1) {
                    description = StringBuilder(descMatch.groupValues[1])
                } else {
                    description = StringBuilder()
                }
            } else if (currentDate != null) {
                // Continue description on next line
                if (line.isNotBlank() && !line.startsWith("Page ")) {
                    description.append(" ").append(line)
                }
            }
        }

        // Add last transaction
        if (currentDate != null && currentAmount != null) {
            val txDateTime = currentDate.atStartOfDay()
            val debit = if (currentAmount < 0) -currentAmount else null
            val credit = if (currentAmount > 0) currentAmount else null

            transactions.add(
                Transaction(
                    dateTime = txDateTime,
                    description = description.toString().trim(),
                    debit = debit,
                    credit = credit,
                    balance = 0.0,
                    reference = null
                )
            )
        }

        return transactions
    }
}
