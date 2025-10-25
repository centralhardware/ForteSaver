package commands

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand

fun BehaviourContext.registerHelpCommand() {
    onCommand("help") { message ->

        val helpMessage = """
            📖 *Forte Bank Statement Parser Bot - Help*

            *Available Commands:*
            /start - Start the bot and see welcome message
            /help - Show this help message

            *How to use:*
            1. Send your Forte Bank statement PDF file to the bot
            2. Wait for the bot to process the file
            3. Receive a formatted summary of your statement with import statistics

            *Extracted Information:*
            • Account holder name
            • Account number
            • Currency
            • Statement period
            • Opening and closing balance
            • All transactions with detailed information:
              - Date, type, amount
              - Merchant name and location
              - MCC code, bank name
              - Payment method (Apple Pay, Google Pay, etc.)

            *Import Statistics:*
            After processing, you'll receive:
            • Total number of transactions found
            • Number of new transactions imported
            • Number of duplicates skipped (if statement was uploaded before)

            *Supported Formats:*
            • PDF files from Forte Bank
        """.trimIndent()

        reply(message, helpMessage)
    }
}
