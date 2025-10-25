package commands

import ForteBankStatementParser
import database.ImportResult
import database.StatementRepository
import database.TransactionData
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDocument
import dev.inmo.tgbotapi.extensions.utils.asDocumentContent
import dev.inmo.tgbotapi.requests.get.GetFile
import kotlinx.coroutines.delay
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

        val progressMessage = reply(message, buildProgressMessage("Downloading PDF...", 0))

        try {
            // Download the file
            logger.info("Downloading PDF file...")
            editMessageText(progressMessage, buildProgressMessage("Downloading PDF...", 5))
            val fileInfo = execute(GetFile(document.media.fileId))
            val fileContent = downloadFile(fileInfo)
            logger.info("PDF file downloaded: ${fileContent.size} bytes")

            // Parse the statement
            logger.info("Starting PDF parsing...")
            editMessageText(progressMessage, buildProgressMessage("Parsing PDF...", 20))
            val statement = ForteBankStatementParser.parse(fileContent)
            logger.info("Parsed statement: ${statement.transactions.size} transactions")

            // Convert to database format
            logger.info("Converting transactions to database format...")
            editMessageText(progressMessage, buildProgressMessage("Converting transactions...", 50))
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
            editMessageText(progressMessage, buildProgressMessage("Saving to database...", 60))

            val importResult = StatementRepository.saveTransactions(transactions) { processed, total ->
                val dbProgress = 60 + (processed * 35 / total) // 60% to 95%
                try {
                    editMessageText(
                        progressMessage,
                        buildProgressMessage("Saving to database ($processed/$total)...", dbProgress)
                    )
                } catch (e: Exception) {
                    // Ignore errors during progress update (e.g., rate limit)
                    logger.debug("Failed to update progress: ${e.message}")
                }
            }
            logger.info("Database save complete")

            editMessageText(progressMessage, buildProgressMessage("Finalizing...", 100))
            delay(500) // Brief pause to show 100%

            // Format and send import statistics
            val response = formatImportResult(importResult)
            editMessageText(progressMessage, response)

        } catch (e: Exception) {
            logger.error("Failed to parse bank statement", e)
            editMessageText(progressMessage, "❌ Failed to parse bank statement: ${e.message}")
        }
    }
}

private fun buildProgressMessage(stage: String, percentage: Int): String {
    val progressBar = buildProgressBar(percentage)
    return """
        ⏳ *Processing Bank Statement*

        $progressBar $percentage%

        $stage
    """.trimIndent()
}

private fun buildProgressBar(percentage: Int): String {
    val totalBlocks = 20
    val filledBlocks = (percentage * totalBlocks / 100).coerceIn(0, totalBlocks)
    val emptyBlocks = totalBlocks - filledBlocks

    return "█".repeat(filledBlocks) + "░".repeat(emptyBlocks)
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
