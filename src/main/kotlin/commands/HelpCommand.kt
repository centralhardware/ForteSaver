package commands

import Config
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand

suspend fun BehaviourContext.registerHelpCommand() {
    onCommand("help") { message ->
        if (Config.ALLOWED_USERS.isNotEmpty() && message.chat.id !in Config.ALLOWED_USERS) {
            reply(message, "Access denied")
            return@onCommand
        }

        val helpMessage = """
            📖 *Forte Bank Statement Parser Bot - Help*

            *Available Commands:*
            /start - Start the bot and see welcome message
            /help - Show this help message

            *How to use:*
            1. Send your Forte Bank statement PDF file to the bot
            2. Wait for the bot to process the file
            3. Receive a formatted summary of your statement

            *Extracted Information:*
            • Account holder name
            • Account number
            • Currency
            • Statement period
            • Opening balance
            • Closing balance
            • List of transactions with dates, descriptions, and amounts
            • Total debits and credits

            *Supported Formats:*
            • PDF files from Forte Bank
        """.trimIndent()

        reply(message, helpMessage)
    }
}
