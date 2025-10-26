package commands

import database.MerchantRepository
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("AutoCategorizeMerchantsCommand")

fun BehaviourContext.registerAutoCategorizeMerchantsCommand() {
    onCommand("autocategorize") { message ->
        logger.info("Starting auto-categorization of merchants")
        
        val initialMessage = reply(message, "🔄 Starting automatic categorization of merchants...")

        try {
            // Get count of merchants needing categorization before processing
            val beforeCount = MerchantRepository.getMerchantsNeedingCategorization().size
            
            // Perform auto-categorization
            val categorizedCount = MerchantRepository.autoCategorizeExistingMerchants()
            
            // Get count of merchants still needing categorization
            val afterCount = MerchantRepository.getMerchantsNeedingCategorization().size
            
            val resultMessage = buildString {
                appendLine("✅ Auto-categorization complete!")
                appendLine()
                appendLine("📊 Results:")
                appendLine("• Merchants processed: $beforeCount")
                appendLine("• Successfully categorized: $categorizedCount")
                appendLine("• Still need manual categorization: $afterCount")
                
                if (categorizedCount > 0) {
                    appendLine()
                    appendLine("💡 Merchants were categorized based on:")
                    appendLine("  - MCC codes (when available)")
                    appendLine("  - Keywords in merchant names")
                }
                
                if (afterCount > 0) {
                    appendLine()
                    appendLine("⚠️ Some merchants could not be automatically categorized.")
                    appendLine("You may need to categorize them manually.")
                }
            }
            
            reply(message, resultMessage)
            logger.info("Auto-categorization completed: $categorizedCount/$beforeCount merchants")
            
        } catch (e: Exception) {
            logger.error("Error during auto-categorization", e)
            reply(message, "❌ Error during auto-categorization: ${e.message}")
        }
    }
}
