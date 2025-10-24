import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId

object Config {
    val BOT_TOKEN: String = System.getenv("BOT_TOKEN")
        ?: error("BOT_TOKEN environment variable is not set")

    val ALLOWED_USERS: List<ChatId> = System.getenv("ALLOWED_USERS")
        ?.split(",")
        ?.map { ChatId(RawChatId(it.trim().toLong())) }
        ?: emptyList()
}
