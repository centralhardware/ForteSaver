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
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDocumentsGroupMessages
import dev.inmo.tgbotapi.extensions.utils.asDocumentContent
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import dev.inmo.tgbotapi.types.message.content.MediaGroupCollectionContent
import dev.inmo.tgbotapi.types.message.content.DocumentMediaGroupPartContent
import dev.inmo.tgbotapi.requests.get.GetFile
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.math.BigDecimal

private val logger = LoggerFactory.getLogger("ParseStatementCommand")

fun BehaviourContext.registerParseStatementCommand() {
    // Handle single documents
    onDocument { message ->
        val document = message.content.asDocumentContent() ?: return@onDocument

        if (document.media.mimeType?.raw != "application/pdf") {
            reply(message, "Please send a PDF file")
            return@onDocument
        }

        processSingleDocument(message, document)
    }

    // Handle document groups (when multiple files are sent together)
    onDocumentsGroupMessages { groupMessage ->
        // groupMessage is a CommonMessage<MediaGroupContent<DocumentMediaGroupPartContent>>
        val mediaGroupContent = groupMessage.content
        val documents = mediaGroupContent.group

        if (documents.isEmpty()) {
            logger.warn("Received empty media group")
            return@onDocumentsGroupMessages
        }

        // Filter only PDF files from wrappers
        val pdfDocuments = documents.filter { wrapper ->
            wrapper.content.media.mimeType?.raw == "application/pdf"
        }

        if (pdfDocuments.isEmpty()) {
            reply(groupMessage, "Please send PDF files only")
            return@onDocumentsGroupMessages
        }

        logger.info("Processing media group: ${pdfDocuments.size} PDF files")
        processDocumentGroupFromWrappers(groupMessage, pdfDocuments)
    }
}

private suspend fun BehaviourContext.processSingleDocument(
    message: dev.inmo.tgbotapi.types.message.abstracts.ContentMessage<*>,
    document: DocumentContent
) {
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

private suspend fun BehaviourContext.processDocumentGroupFromWrappers(
    message: dev.inmo.tgbotapi.types.message.abstracts.ContentMessage<*>,
    wrappers: List<MediaGroupCollectionContent.PartWrapper<DocumentMediaGroupPartContent>>
) {
    val progressMessage = reply(message, buildGroupProgressMessage(0, wrappers.size, "Starting...", 0))

    val allResults = mutableListOf<ImportResult>()
    var successCount = 0
    var failureCount = 0

    try {
        wrappers.forEachIndexed { index, wrapper ->
            val currentFile = index + 1
            logger.info("Processing file $currentFile of ${wrappers.size}")

            try {
                val documentContent = wrapper.content as? DocumentContent
                if (documentContent == null) {
                    logger.warn("Wrapper $currentFile does not contain a document")
                    failureCount++
                    return@forEachIndexed
                }

                // Download the file
                editMessageText(
                    progressMessage,
                    buildGroupProgressMessage(currentFile, wrappers.size, "Downloading...", 5)
                )
                val fileInfo = execute(GetFile(documentContent.media.fileId))
                val fileContent = downloadFile(fileInfo)
                logger.info("File $currentFile downloaded: ${fileContent.size} bytes")

                // Parse the statement
                editMessageText(
                    progressMessage,
                    buildGroupProgressMessage(currentFile, wrappers.size, "Parsing...", 20)
                )
                val statement = ForteBankStatementParser.parse(fileContent)
                logger.info("File $currentFile parsed: ${statement.transactions.size} transactions")

                // Convert to database format
                editMessageText(
                    progressMessage,
                    buildGroupProgressMessage(currentFile, wrappers.size, "Converting...", 50)
                )
                val transactions = statement.transactions.map { tx ->
                    TransactionData(
                        date = tx.date,
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

                // Save to database
                editMessageText(
                    progressMessage,
                    buildGroupProgressMessage(currentFile, wrappers.size, "Saving to database...", 60)
                )
                val importResult = StatementRepository.saveTransactions(transactions) { _, _ ->
                    // Skip individual progress updates for group processing to avoid rate limits
                }
                logger.info("File $currentFile saved: ${importResult.importedTransactions} imported")

                allResults.add(importResult)
                successCount++

                // Show completion for this file
                editMessageText(
                    progressMessage,
                    buildGroupProgressMessage(currentFile, wrappers.size, "Completed", 100)
                )
                delay(300)

            } catch (e: Exception) {
                logger.error("Failed to process file $currentFile", e)
                failureCount++
                editMessageText(
                    progressMessage,
                    buildGroupProgressMessage(
                        currentFile,
                        wrappers.size,
                        "❌ Error: ${e.message?.take(50)}",
                        0
                    )
                )
                delay(1000)
            }
        }

        // Show final results
        val totalResult = ImportResult(
            totalTransactions = allResults.sumOf { it.totalTransactions },
            importedTransactions = allResults.sumOf { it.importedTransactions },
            duplicateTransactions = allResults.sumOf { it.duplicateTransactions }
        )

        val response = formatGroupImportResult(totalResult, successCount, failureCount, wrappers.size)
        editMessageText(progressMessage, response)

    } catch (e: Exception) {
        logger.error("Failed to process document group", e)
        editMessageText(progressMessage, "❌ Failed to process documents: ${e.message}")
    }
}

private suspend fun BehaviourContext.processDocumentGroup(
    message: dev.inmo.tgbotapi.types.message.abstracts.ContentMessage<*>,
    messages: List<dev.inmo.tgbotapi.types.message.abstracts.ContentMessage<*>>
) {
    val progressMessage = reply(message, buildGroupProgressMessage(0, messages.size, "Starting...", 0))

    val allResults = mutableListOf<ImportResult>()
    var successCount = 0
    var failureCount = 0

    try {
        messages.forEachIndexed { index, docMessage ->
            val currentFile = index + 1
            logger.info("Processing file $currentFile of ${messages.size}")

            try {
                val documentContent = docMessage.content as? DocumentContent
                if (documentContent == null) {
                    logger.warn("Message $currentFile does not contain a document")
                    failureCount++
                    return@forEachIndexed
                }

                // Download the file
                editMessageText(
                    progressMessage,
                    buildGroupProgressMessage(currentFile, messages.size, "Downloading...", 5)
                )
                val fileInfo = execute(GetFile(documentContent.media.fileId))
                val fileContent = downloadFile(fileInfo)
                logger.info("File $currentFile downloaded: ${fileContent.size} bytes")

                // Parse the statement
                editMessageText(
                    progressMessage,
                    buildGroupProgressMessage(currentFile, messages.size, "Parsing...", 20)
                )
                val statement = ForteBankStatementParser.parse(fileContent)
                logger.info("File $currentFile parsed: ${statement.transactions.size} transactions")

                // Convert to database format
                editMessageText(
                    progressMessage,
                    buildGroupProgressMessage(currentFile, messages.size, "Converting...", 50)
                )
                val transactions = statement.transactions.map { tx ->
                    TransactionData(
                        date = tx.date,
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

                // Save to database
                editMessageText(
                    progressMessage,
                    buildGroupProgressMessage(currentFile, messages.size, "Saving to database...", 60)
                )
                val importResult = StatementRepository.saveTransactions(transactions) { _, _ ->
                    // Skip individual progress updates for group processing to avoid rate limits
                }
                logger.info("File $currentFile saved: ${importResult.importedTransactions} imported")

                allResults.add(importResult)
                successCount++

                // Show completion for this file
                editMessageText(
                    progressMessage,
                    buildGroupProgressMessage(currentFile, messages.size, "Completed", 100)
                )
                delay(300)

            } catch (e: Exception) {
                logger.error("Failed to process file $currentFile", e)
                failureCount++
                editMessageText(
                    progressMessage,
                    buildGroupProgressMessage(
                        currentFile,
                        messages.size,
                        "❌ Error: ${e.message?.take(50)}",
                        0
                    )
                )
                delay(1000)
            }
        }

        // Show final results
        val totalResult = ImportResult(
            totalTransactions = allResults.sumOf { it.totalTransactions },
            importedTransactions = allResults.sumOf { it.importedTransactions },
            duplicateTransactions = allResults.sumOf { it.duplicateTransactions }
        )

        val response = formatGroupImportResult(totalResult, successCount, failureCount, messages.size)
        editMessageText(progressMessage, response)

    } catch (e: Exception) {
        logger.error("Failed to process document group", e)
        editMessageText(progressMessage, "❌ Failed to process documents: ${e.message}")
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

private fun buildGroupProgressMessage(currentFile: Int, totalFiles: Int, stage: String, percentage: Int): String {
    val progressBar = buildProgressBar(percentage)
    return """
        ⏳ *Processing Multiple Bank Statements*

        📁 File $currentFile of $totalFiles

        $progressBar $percentage%

        $stage
    """.trimIndent()
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

private fun formatGroupImportResult(
    result: ImportResult,
    successCount: Int,
    failureCount: Int,
    totalFiles: Int
): String {
    val sb = StringBuilder()

    sb.appendLine("✅ *Batch Import Complete*")
    sb.appendLine()
    sb.appendLine("📁 *Files Processed:*")
    sb.appendLine("• Total files: $totalFiles")
    sb.appendLine("• ✅ Successful: $successCount")
    if (failureCount > 0) {
        sb.appendLine("• ❌ Failed: $failureCount")
    }
    sb.appendLine()
    sb.appendLine("📊 *Total Import Statistics:*")
    sb.appendLine("• Total transactions: ${result.totalTransactions}")
    sb.appendLine("• ✅ Imported: ${result.importedTransactions}")

    if (result.duplicateTransactions > 0) {
        sb.appendLine("• ⚠️ Duplicates skipped: ${result.duplicateTransactions}")
    }

    return sb.toString()
}
