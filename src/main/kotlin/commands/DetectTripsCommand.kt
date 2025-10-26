package commands

import database.AccountRepository
import database.TripRepository
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import org.slf4j.LoggerFactory
import services.TripDetectionService
import java.time.format.DateTimeFormatter

private val logger = LoggerFactory.getLogger("DetectTripsCommand")

/**
 * Register /detecttrips command.
 * Detects trips from transaction patterns and saves them to database.
 */
fun BehaviourContext.registerDetectTripsCommand() {
    onCommand("detecttrips") { message ->
        try {
            logger.info("User ${message.chat.id} requested trip detection")
            
            sendMessage(
                message.chat,
                "🔍 Detecting trips from your transactions..."
            )

            // Get all accounts
            val accounts = AccountRepository.getAllAccounts()
            
            if (accounts.isEmpty()) {
                sendMessage(
                    message.chat,
                    "No accounts found. Please import transactions first using /parse"
                )
                return@onCommand
            }

            var totalTripsDetected = 0
            val tripsByAccount = mutableMapOf<String, Int>()

            // Detect trips for each account
            for (account in accounts) {
                logger.info("Detecting trips for account: ${account.accountNumber}")
                
                // Clear existing trips for this account
                TripDetectionService.clearTrips(account.id)
                
                // Detect new trips
                val trips = TripDetectionService.detectTrips(
                    accountId = account.id,
                    maxTripDuration = 30,  // Max 30 days per trip
                    minGapDays = 30         // At least 30 days between trips to same location
                )
                
                if (trips.isNotEmpty()) {
                    // Save trips to database
                    val savedCount = TripDetectionService.saveTrips(trips)
                    totalTripsDetected += savedCount
                    tripsByAccount[account.accountNumber] = savedCount
                    
                    logger.info("Detected and saved $savedCount trips for account ${account.accountNumber}")
                }
            }

            // Send result
            if (totalTripsDetected > 0) {
                val resultMessage = buildString {
                    appendLine("✅ Trip detection completed!")
                    appendLine()
                    appendLine("📊 Summary:")
                    appendLine("Total trips detected: $totalTripsDetected")
                    appendLine()
                    
                    if (tripsByAccount.isNotEmpty()) {
                        appendLine("By account:")
                        tripsByAccount.forEach { (accountNum, count) ->
                            appendLine("  • $accountNum: $count trips")
                        }
                    }
                    
                    appendLine()
                    appendLine("Use /trips to view all detected trips")
                }
                
                sendMessage(message.chat, resultMessage)
            } else {
                sendMessage(
                    message.chat,
                    "No trips detected. Make sure you have transactions in foreign locations."
                )
            }
            
        } catch (e: Exception) {
            logger.error("Error detecting trips", e)
            sendMessage(
                message.chat,
                "❌ Error detecting trips: ${e.message}"
            )
        }
    }
}

