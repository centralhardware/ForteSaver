import commands.registerAutoCategorizeMerchantsCommand
import commands.registerParseStatementCommand
import commands.registerStartCommand
import database.DatabaseManager
import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.AppConfig
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.longPolling
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.utils.RiskFeature
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Main")

@OptIn(Warning::class, RiskFeature::class)
suspend fun main() {
    logger.info("Starting Forte Bank Statement Parser Bot...")

    // Initialize database
    DatabaseManager.init(Config.DATABASE_URL)
    logger.info("Database initialized")

    AppConfig.init("forte-saver")

    longPolling({ restrictAccess(EnvironmentVariableUserAccessChecker()) }) {
        logger.info("Bot started successfully")

        // Set bot commands
        setMyCommands(
            BotCommand("start", "Start the bot"),
            BotCommand("help", "Show help message"),
            BotCommand("autocategorize", "Auto-categorize merchants")
        )

        // Register command handlers
        registerStartCommand()
        registerParseStatementCommand()
        registerAutoCategorizeMerchantsCommand()

        logger.info("All handlers registered")
    }.second.join()
}
