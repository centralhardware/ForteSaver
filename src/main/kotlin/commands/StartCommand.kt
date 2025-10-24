package commands

import Config
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand

suspend fun BehaviourContext.registerStartCommand() {
    onCommand("start") { message ->
        if (Config.ALLOWED_USERS.isNotEmpty() && message.chat.id !in Config.ALLOWED_USERS) {
            reply(message, "Access denied")
            return@onCommand
        }

        val welcomeMessage = """
            👋 Welcome to Forte Bank Statement Parser Bot!

            📄 Send me your bank statement PDF file and I will extract the following information:

            • Account holder
            • Account number
            • Period
            • Opening and closing balances
            • All transactions

            Simply upload your Forte Bank statement PDF to get started.
        """.trimIndent()

        reply(message, welcomeMessage)
    }
}
