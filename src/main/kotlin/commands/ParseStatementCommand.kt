package commands

import BankStatement
import Config
import ForteBankStatementParser
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDocument
import dev.inmo.tgbotapi.extensions.utils.asDocumentContent
import dev.inmo.tgbotapi.requests.get.GetFile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.text.NumberFormat
import java.util.*

private val logger = LoggerFactory.getLogger("ParseStatementCommand")

suspend fun BehaviourContext.registerParseStatementCommand() {
    onDocument { message ->
        if (Config.ALLOWED_USERS.isNotEmpty() && message.chat.id !in Config.ALLOWED_USERS) {
            reply(message, "Access denied")
            return@onDocument
        }

        val document = message.content.asDocumentContent() ?: return@onDocument

        if (document.media.mimeType?.raw != "application/pdf") {
            reply(message, "Please send a PDF file")
            return@onDocument
        }

        reply(message, "Processing your bank statement...")

        try {
            // Download the file
            val fileInfo = execute(GetFile(document.media.fileId))
            val fileContent = downloadFile(fileInfo)

            // Parse the statement
            val statement = ForteBankStatementParser.parse(fileContent)

            // Format the result
            val response = formatBankStatement(statement)

            // Send back the result
            reply(message, response)

            // Also send JSON if requested
            // Uncomment if you want to send JSON as well
            // val json = Json.encodeToString(statement)
            // reply(message, "JSON:\n```json\n$json\n```")

        } catch (e: Exception) {
            logger.error("Failed to parse bank statement", e)
            reply(message, "Failed to parse bank statement: ${e.message}")
        }
    }
}

private fun formatBankStatement(statement: BankStatement): String {
    val sb = StringBuilder()
    val numberFormat = NumberFormat.getInstance(Locale.of("ru", "RU"))

    sb.appendLine("📊 *Bank Statement*")
    sb.appendLine()
    sb.appendLine("👤 *Account Holder:* ${statement.accountHolder}")
    sb.appendLine("💳 *Account:* ${statement.accountNumber}")
    sb.appendLine("💵 *Currency:* ${statement.currency}")
    sb.appendLine("📅 *Period:* ${statement.period.from} - ${statement.period.to}")
    sb.appendLine()
    sb.appendLine("💰 *Opening Balance:* ${numberFormat.format(statement.openingBalance)} ${statement.currency}")
    sb.appendLine("💰 *Closing Balance:* ${numberFormat.format(statement.closingBalance)} ${statement.currency}")
    sb.appendLine()

    if (statement.transactions.isNotEmpty()) {
        sb.appendLine("📝 *Transactions:* ${statement.transactions.size}")
        sb.appendLine()

        // Calculate total debit and credit
        val totalDebit = statement.transactions.sumOf { it.debit ?: 0.0 }
        val totalCredit = statement.transactions.sumOf { it.credit ?: 0.0 }

        sb.appendLine("📉 *Total Debits:* ${numberFormat.format(totalDebit)} ${statement.currency}")
        sb.appendLine("📈 *Total Credits:* ${numberFormat.format(totalCredit)} ${statement.currency}")
        sb.appendLine()

        sb.appendLine("*Recent Transactions:*")
        statement.transactions.take(10).forEach { tx ->
            val amount = if (tx.debit != null) {
                "-${numberFormat.format(tx.debit)}"
            } else {
                "+${numberFormat.format(tx.credit)}"
            }
            sb.appendLine("${tx.dateTime.toLocalDate()} - ${tx.description}: $amount ${statement.currency}")
        }

        if (statement.transactions.size > 10) {
            sb.appendLine()
            sb.appendLine("... and ${statement.transactions.size - 10} more transactions")
        }
    } else {
        sb.appendLine("No transactions found")
    }

    return sb.toString()
}
