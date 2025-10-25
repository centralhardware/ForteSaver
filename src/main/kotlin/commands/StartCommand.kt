package commands

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand

fun BehaviourContext.registerStartCommand() {
    onCommand("start") { message ->
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
