package commands

import ForteBankStatementParser
import database.ImportResult
import database.StatementRepository
import database.TransactionData
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDocument
import dev.inmo.tgbotapi.extensions.utils.asDocumentContent
import dev.inmo.tgbotapi.requests.get.GetFile
import org.slf4j.LoggerFactory
import java.math.BigDecimal

private val logger = LoggerFactory.getLogger("ParseStatementCommand")

fun BehaviourContext.registerParseStatementCommand() {
    onDocument { message ->
        val document = message.content.asDocumentContent() ?: return@onDocument

        if (document.media.mimeType?.raw != "application/pdf") {
            reply(message, "Please send a PDF file")
            return@onDocument
        }

        reply(message, "Processing your bank statement...")

        try {
            // Download the file
            logger.info("Downloading PDF file...")
            val fileInfo = execute(GetFile(document.media.fileId))
            val fileContent = downloadFile(fileInfo)
            logger.info("PDF file downloaded: ${fileContent.size} bytes")

            // Parse the statement
            logger.info("Starting PDF parsing...")
            val statement = ForteBankStatementParser.parse(fileContent)
            logger.info("Parsed statement: ${statement.transactions.size} transactions")

            // Convert to database format
            logger.info("Converting transactions to database format...")
            val transactions = statement.transactions.map { tx ->
                TransactionData(
                    date = tx.date,
                    type = tx.type,
                    amount = BigDecimal.valueOf(tx.amount),
                    currency = tx.currency,
                    transactionAmount = tx.transactionAmount?.let { BigDecimal.valueOf(it) },
                    transactionCurrency = tx.transactionCurrency,
                    merchantName = tx.merchantName,
                    merchantLocation = tx.merchantLocation,
                    mccCode = tx.mccCode,
                    bankName = tx.bankName,
                    paymentMethod = tx.paymentMethod,
                    accountNumber = statement.accountNumber,
                    description = tx.description
                )
            }
            logger.info("Conversion complete: ${transactions.size} transactions ready for import")

            // Save to database (uses daily sequence for deduplication)
            logger.info("Saving transactions to database...")
            val importResult = StatementRepository.saveTransactions(transactions)
            logger.info("Database save complete")

            // Format and send import statistics
            val response = formatImportResult(importResult)
            reply(message, response)

        } catch (e: Exception) {
            logger.error("Failed to parse bank statement", e)
            reply(message, "Failed to parse bank statement: ${e.message}")
        }
    }
}

private fun formatImportResult(result: ImportResult): String {
    val sb = StringBuilder()

    sb.appendLine("✅ *Import Complete*")
    sb.appendLine()
    sb.appendLine("📊 *Import Statistics:*")
    sb.appendLine("• Total transactions: ${result.totalTransactions}")
    sb.appendLine("• ✅ Imported: ${result.importedTransactions}")

    if (result.duplicateTransactions > 0) {
        sb.appendLine("• ⚠️ Duplicates skipped: ${result.duplicateTransactions}")
    }

    return sb.toString()
}
