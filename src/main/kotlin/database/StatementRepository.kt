package database

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.LocalDateTime as JavaLocalDateTime

data class ImportResult(
    val totalTransactions: Int,
    val importedTransactions: Int,
    val duplicateTransactions: Int
)

object StatementRepository {
    private val logger = LoggerFactory.getLogger(StatementRepository::class.java)

    suspend fun saveTransactions(
        transactions: List<TransactionData>,
        onProgress: suspend (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): ImportResult = newSuspendedTransaction(Dispatchers.IO) {
        val totalCount = transactions.size

        // Group transactions by (currency, date) and assign daily sequence numbers
        val transactionsWithMetadata = transactions
            .groupBy { it.currency to it.date }
            .flatMap { (currencyDatePair, txsOnDate) ->
                val (currency, date) = currencyDatePair
                txsOnDate.mapIndexed { index, tx ->
                    TransactionWithMetadata(
                        data = tx,
                        dailySequence = index,  // 0, 1, 2... within this day for this currency
                        hash = calculateTransactionHash(tx)
                    )
                }
            }

        // Get all unique currencies and hashes from the batch
        val currencies = transactionsWithMetadata.map { it.data.currency }.distinct()
        val hashes = transactionsWithMetadata.map { it.hash }.distinct()

        // Check which (currency, daily_sequence, hash) triples already exist in database
        val existingTriples = if (transactionsWithMetadata.isNotEmpty()) {
            Transactions
                .select(Transactions.currency, Transactions.dailySequence, Transactions.transactionHash)
                .where {
                    (Transactions.currency inList currencies) and (Transactions.transactionHash inList hashes)
                }
                .map { row ->
                    Triple(
                        row[Transactions.currency],
                        row[Transactions.dailySequence],
                        row[Transactions.transactionHash]
                    )
                }
                .toSet()
        } else {
            emptySet()
        }

        // Insert only transactions that don't exist (by currency + daily_sequence + hash)
        var importedCount = 0
        var processedCount = 0
        val totalToProcess = transactionsWithMetadata.size

        logger.info("Starting import of $totalToProcess transactions...")

        transactionsWithMetadata.forEach { txMeta ->
            val tx = txMeta.data
            val triple = Triple(tx.currency, txMeta.dailySequence, txMeta.hash)

            if (triple !in existingTriples) {
                Transactions.insert {
                    it[Transactions.transactionDate] = tx.date
                    it[Transactions.transactionType] = tx.type
                    it[Transactions.amount] = tx.amount
                    it[Transactions.currency] = tx.currency
                    it[Transactions.transactionAmount] = tx.transactionAmount
                    it[Transactions.transactionCurrency] = tx.transactionCurrency
                    it[Transactions.merchantName] = tx.merchantName
                    it[Transactions.merchantLocation] = tx.merchantLocation
                    it[Transactions.mccCode] = tx.mccCode
                    it[Transactions.bankName] = tx.bankName
                    it[Transactions.paymentMethod] = tx.paymentMethod
                    it[Transactions.accountNumber] = tx.accountNumber
                    it[Transactions.description] = tx.description
                    it[Transactions.dailySequence] = txMeta.dailySequence
                    it[Transactions.transactionHash] = txMeta.hash
                    it[Transactions.createdAt] = JavaLocalDateTime.now()
                }
                importedCount++
            }

            processedCount++

            // Log progress every 10% or at specific milestones
            val percentage = (processedCount * 100) / totalToProcess
            if (processedCount % (totalToProcess / 10).coerceAtLeast(1) == 0 || processedCount == totalToProcess) {
                logger.info("Import progress: $processedCount/$totalToProcess ($percentage%) - $importedCount new, ${processedCount - importedCount} duplicates")
                onProgress(processedCount, totalToProcess)
            }
        }

        val duplicates = totalCount - importedCount

        logger.info(
            "Import complete: $importedCount new, $duplicates duplicates out of $totalCount total"
        )

        ImportResult(
            totalTransactions = totalCount,
            importedTransactions = importedCount,
            duplicateTransactions = duplicates
        )
    }

    private fun calculateTransactionHash(tx: TransactionData): String {
        val hashData = buildString {
            append(tx.date)
            append(tx.type)
            append(tx.amount)
            append(tx.transactionAmount ?: "")
            append(tx.transactionCurrency ?: "")
            append(tx.merchantName ?: "")
            append(tx.merchantLocation ?: "")
            append(tx.mccCode ?: "")
            append(tx.bankName ?: "")
            append(tx.paymentMethod ?: "")
            append(tx.description)
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(hashData.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

data class TransactionData(
    val date: java.time.LocalDate,
    val type: String,

    // Amount in account currency
    val amount: BigDecimal,
    val currency: String,

    // Amount in transaction currency (optional)
    val transactionAmount: BigDecimal?,
    val transactionCurrency: String?,

    // Merchant details
    val merchantName: String?,
    val merchantLocation: String?,

    // Payment details
    val mccCode: String?,
    val bankName: String?,
    val paymentMethod: String?,

    // Account information
    val accountNumber: String?,

    // Full description
    val description: String
)

private data class TransactionWithMetadata(
    val data: TransactionData,
    val dailySequence: Int,  // Sequence within the same day for this currency (0, 1, 2...)
    val hash: String
)
