package database

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Repository for managing trips and their transactions.
 */
object TripRepository {

    data class TripData(
        val id: Int,
        val accountId: Int,
        val destinationCountry: String?,
        val destinationCity: String?,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val totalAmount: BigDecimal?,
        val currency: String?,
        val confidenceScore: BigDecimal?,
        val transactionCount: Int
    )

    /**
     * Get all trips for an account.
     */
    suspend fun getTripsForAccount(accountId: Int): List<TripData> = newSuspendedTransaction(Dispatchers.IO) {
        Trips
            .selectAll()
            .where { Trips.accountId eq accountId }
            .orderBy(Trips.startDate to SortOrder.DESC)
            .map { row ->
                val tripId = row[Trips.id].value
                val transactionCount = Transactions
                    .selectAll()
                    .where { Transactions.tripId eq tripId }
                    .count()
                    .toInt()

                TripData(
                    id = tripId,
                    accountId = row[Trips.accountId].value,
                    destinationCountry = row[Trips.destinationCountry],
                    destinationCity = row[Trips.destinationCity],
                    startDate = row[Trips.startDate],
                    endDate = row[Trips.endDate],
                    totalAmount = row[Trips.totalAmount],
                    currency = row[Trips.currency],
                    confidenceScore = row[Trips.confidenceScore],
                    transactionCount = transactionCount
                )
            }
    }

    /**
     * Get all trips across all accounts.
     */
    suspend fun getAllTrips(): List<TripData> = newSuspendedTransaction(Dispatchers.IO) {
        Trips
            .selectAll()
            .orderBy(Trips.startDate to SortOrder.DESC)
            .map { row ->
                val tripId = row[Trips.id].value
                val transactionCount = Transactions
                    .selectAll()
                    .where { Transactions.tripId eq tripId }
                    .count()
                    .toInt()

                TripData(
                    id = tripId,
                    accountId = row[Trips.accountId].value,
                    destinationCountry = row[Trips.destinationCountry],
                    destinationCity = row[Trips.destinationCity],
                    startDate = row[Trips.startDate],
                    endDate = row[Trips.endDate],
                    totalAmount = row[Trips.totalAmount],
                    currency = row[Trips.currency],
                    confidenceScore = row[Trips.confidenceScore],
                    transactionCount = transactionCount
                )
            }
    }

    /**
     * Get transactions for a specific trip.
     */
    suspend fun getTransactionsForTrip(tripId: Int): List<TransactionSummary> =
        newSuspendedTransaction(Dispatchers.IO) {
            (Transactions innerJoin Merchants)
                .selectAll()
                .where { Transactions.tripId eq tripId }
                .orderBy(Transactions.transactionDate to SortOrder.ASC)
                .map { row ->
                    TransactionSummary(
                        id = row[Transactions.id].value,
                        date = row[Transactions.transactionDate],
                        amount = row[Transactions.amount],
                        merchantName = row[Merchants.name],
                        merchantLocation = row[Merchants.location],
                        description = row[Transactions.description]
                    )
                }
        }

    /**
     * Get trip statistics for an account.
     */
    suspend fun getTripStats(accountId: Int): TripStats = newSuspendedTransaction(Dispatchers.IO) {
        val trips = Trips.selectAll().where { Trips.accountId eq accountId }.toList()

        val totalTrips = trips.count()
        val totalSpent = trips.sumOf { it[Trips.totalAmount] ?: BigDecimal.ZERO }

        // Group by destination
        val byCountry = trips
            .groupBy { it[Trips.destinationCountry] }
            .mapValues { it.value.count().toLong() }
            .filterKeys { it != null }
            .mapKeys { it.key!! }

        TripStats(
            totalTrips = totalTrips,
            totalSpent = totalSpent,
            tripsByCountry = byCountry
        )
    }

    data class TransactionSummary(
        val id: Int,
        val date: LocalDate,
        val amount: BigDecimal,
        val merchantName: String,
        val merchantLocation: String?,
        val description: String?
    )

    data class TripStats(
        val totalTrips: Int,
        val totalSpent: BigDecimal,
        val tripsByCountry: Map<String, Long>
    )
}
