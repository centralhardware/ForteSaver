package services

import database.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Service for detecting trips from transaction patterns.
 * 
 * A trip is detected when:
 * 1. Multiple consecutive transactions occur in a foreign country
 * 2. Transactions are within a short time period (typically <= 7 days)
 * 3. There's a clear gap before/after the trip (no transactions in that location)
 */
object TripDetectionService {

    data class TripCandidate(
        val accountId: Int,
        val destinationCountry: String,
        val destinationCity: String?,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val transactionIds: List<Int>,
        val totalAmount: BigDecimal,
        val currency: String,
        val confidence: Double
    )

    /**
     * Detect all trips for a given account.
     * 
     * @param accountId Account to analyze
     * @param maxTripDuration Maximum days for a single trip (default 30)
     * @param minGapDays Minimum days gap to separate trips (default 30)
     * @return List of detected trip candidates
     */
    suspend fun detectTrips(
        accountId: Int,
        maxTripDuration: Int = 30,
        minGapDays: Int = 30
    ): List<TripCandidate> = newSuspendedTransaction(Dispatchers.IO) {
        
        // Get all transactions with foreign locations for this account
        val foreignTransactions = (Transactions innerJoin Merchants innerJoin Accounts)
            .selectAll()
            .where {
                (Transactions.accountId eq accountId) and
                Merchants.countryCode.isNotNull() and
                Transactions.merchantId.isNotNull()
            }
            .orderBy(Transactions.transactionDate to SortOrder.ASC)
            .map { row ->
                ForeignTransaction(
                    id = row[Transactions.id].value,
                    date = row[Transactions.transactionDate],
                    amount = row[Transactions.amount],
                    currency = row[Accounts.currency], // Account currency
                    countryCode = row[Merchants.countryCode]!!,
                    city = row[Merchants.city]
                )
            }

        if (foreignTransactions.isEmpty()) {
            return@newSuspendedTransaction emptyList()
        }

        // Group transactions by location and detect continuous periods
        val trips = mutableListOf<TripCandidate>()
        
        // Group by country and city
        val locationGroups = foreignTransactions.groupBy { 
            LocationKey(it.countryCode, it.city)
        }

        locationGroups.forEach { (location, transactions) ->
            // Sort by date
            val sorted = transactions.sortedBy { it.date }
            
            // Detect continuous trip periods
            var tripStart = 0
            
            for (i in 1 until sorted.size) {
                val current = sorted[i]
                val previous = sorted[i - 1]
                
                // Calculate gap in days
                val daysBetween = ChronoUnit.DAYS.between(previous.date, current.date)
                
                // If gap is too large, this is a new trip
                if (daysBetween > minGapDays) {
                    // Save previous trip
                    val tripTransactions = sorted.subList(tripStart, i)
                    
                    if (tripTransactions.isNotEmpty()) {
                        val trip = createTripCandidate(
                            accountId,
                            location,
                            tripTransactions,
                            maxTripDuration
                        )
                        
                        if (trip != null) {
                            trips.add(trip)
                        }
                    }
                    
                    // Start new trip
                    tripStart = i
                }
            }
            
            // Don't forget the last trip
            val tripTransactions = sorted.subList(tripStart, sorted.size)
            if (tripTransactions.isNotEmpty()) {
                val trip = createTripCandidate(
                    accountId,
                    location,
                    tripTransactions,
                    maxTripDuration
                )
                
                if (trip != null) {
                    trips.add(trip)
                }
            }
        }

        trips
    }

    /**
     * Create a trip candidate from a group of transactions.
     */
    private fun createTripCandidate(
        accountId: Int,
        location: LocationKey,
        transactions: List<ForeignTransaction>,
        maxTripDuration: Int
    ): TripCandidate? {
        if (transactions.isEmpty()) return null

        val startDate = transactions.first().date
        val endDate = transactions.last().date
        val duration = ChronoUnit.DAYS.between(startDate, endDate)

        // Filter out trips that are too long (likely not a trip)
        if (duration > maxTripDuration) {
            return null
        }

        // Calculate total spending
        val totalAmount = transactions.sumOf { it.amount }
        val currency = transactions.first().currency

        // Calculate confidence score
        val confidence = calculateConfidence(transactions, duration)

        return TripCandidate(
            accountId = accountId,
            destinationCountry = location.countryCode,
            destinationCity = location.city,
            startDate = startDate,
            endDate = endDate,
            transactionIds = transactions.map { it.id },
            totalAmount = totalAmount,
            currency = currency,
            confidence = confidence
        )
    }

    /**
     * Calculate confidence score for trip detection (0.0 to 1.0).
     * 
     * Higher confidence when:
     * - More transactions (indicates real activity, not just passing through)
     * - Shorter duration (typical trip length)
     * - Daily spending (continuous activity)
     */
    private fun calculateConfidence(transactions: List<ForeignTransaction>, duration: Long): Double {
        var confidence = 0.5 // Base confidence

        // More transactions = higher confidence
        when {
            transactions.size >= 10 -> confidence += 0.3
            transactions.size >= 5 -> confidence += 0.2
            transactions.size >= 3 -> confidence += 0.1
        }

        // Typical trip duration (1-14 days) = higher confidence
        when {
            duration in 1..14 -> confidence += 0.2
            duration in 15..30 -> confidence += 0.1
            duration > 30 -> confidence -= 0.2
        }

        // Daily activity (transactions on most days) = higher confidence
        val uniqueDays = transactions.map { it.date }.toSet().size
        val coverage = uniqueDays.toDouble() / (duration + 1)
        
        when {
            coverage >= 0.7 -> confidence += 0.2
            coverage >= 0.5 -> confidence += 0.1
        }

        return confidence.coerceIn(0.0, 1.0)
    }

    /**
     * Save detected trips to database.
     */
    suspend fun saveTrips(trips: List<TripCandidate>): Int = newSuspendedTransaction(Dispatchers.IO) {
        var savedCount = 0

        trips.forEach { trip ->
            // Insert trip
            val tripId = Trips.insertAndGetId {
                it[accountId] = trip.accountId
                it[destinationCountry] = trip.destinationCountry
                it[destinationCity] = trip.destinationCity
                it[startDate] = trip.startDate
                it[endDate] = trip.endDate
                it[totalAmount] = trip.totalAmount
                it[currency] = trip.currency
                it[confidenceScore] = BigDecimal.valueOf(trip.confidence)
                it[detectedAt] = LocalDateTime.now()
                it[createdAt] = LocalDateTime.now()
            }

            // Link transactions to trip
            trip.transactionIds.forEach { transactionId ->
                Transactions.update({ Transactions.id eq transactionId }) {
                    it[Transactions.tripId] = tripId.value
                }
            }

            savedCount++
        }

        savedCount
    }

    /**
     * Clear all trips for an account (for re-detection).
     */
    suspend fun clearTrips(accountId: Int) = newSuspendedTransaction(Dispatchers.IO) {
        // Clear trip references from transactions
        Transactions.update({
            (Transactions.accountId eq accountId) and (Transactions.tripId.isNotNull())
        }) {
            it[Transactions.tripId] = null
        }

        // Delete trips
        Trips.deleteWhere { Trips.accountId eq accountId }
    }

    private data class ForeignTransaction(
        val id: Int,
        val date: LocalDate,
        val amount: BigDecimal,
        val currency: String,
        val countryCode: String,
        val city: String?
    )

    private data class LocationKey(
        val countryCode: String,
        val city: String?
    )
}
