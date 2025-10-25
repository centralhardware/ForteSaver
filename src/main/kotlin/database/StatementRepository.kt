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

        // Group transactions by (account, date) and assign daily sequence numbers
        val transactionsWithMetadata = transactions
            .groupBy { Pair(it.accountNumber, it.currency) to it.date }
            .flatMap { (accountDatePair, txsOnDate) ->
                txsOnDate.mapIndexed { index, tx ->
                    TransactionWithMetadata(
                        data = tx,
                        dailySequence = index,  // 0, 1, 2... within this day for this account
                        hash = calculateTransactionHash(tx)
                    )
                }
            }

        // Get all unique account identifiers and hashes from the batch
        val accountIdentifiers = transactionsWithMetadata
            .map { Pair(it.data.accountNumber, it.data.currency) }
            .distinct()

        // Find or create all accounts first
        val accountIds = accountIdentifiers.associateWith { (accountNumber, currency) ->
            AccountRepository.findOrCreateAccount(accountNumber, currency)
        }

        val hashes = transactionsWithMetadata.map { it.hash }.distinct()

        // Check which (account_id, daily_sequence, hash) triples already exist in database
        val existingTriples = if (transactionsWithMetadata.isNotEmpty()) {
            val allAccountIds = accountIds.values.toSet()
            Transactions
                .select(Transactions.accountId, Transactions.dailySequence, Transactions.transactionHash)
                .where {
                    (Transactions.accountId inList allAccountIds) and (Transactions.transactionHash inList hashes)
                }
                .map { row ->
                    Triple(
                        row[Transactions.accountId].value,
                        row[Transactions.dailySequence],
                        row[Transactions.transactionHash]
                    )
                }
                .toSet()
        } else {
            emptySet()
        }

        // Insert only transactions that don't exist (by account_id + daily_sequence + hash)
        var importedCount = 0
        var processedCount = 0
        val totalToProcess = transactionsWithMetadata.size

        logger.info("Starting import of $totalToProcess transactions...")

        transactionsWithMetadata.forEach { txMeta ->
            val tx = txMeta.data

            // Get account ID from pre-cached map
            val accountKey = Pair(tx.accountNumber, tx.currency)
            val accountId = accountIds[accountKey]
                ?: throw IllegalStateException("Account ID not found for $accountKey")

            val triple = Triple(accountId, txMeta.dailySequence, txMeta.hash)

            if (triple !in existingTriples) {
                // Find or create merchant for this transaction
                val merchantId = MerchantRepository.findOrCreateMerchant(
                    name = tx.merchantName,
                    location = tx.merchantLocation,
                    mccCode = tx.mccCode
                )

                // Find or create bank (this is the merchant's bank, not Forte)
                val bankId = AccountRepository.findOrCreateBank(tx.bankName)

                Transactions.insert {
                    it[Transactions.transactionDate] = tx.date
                    it[Transactions.amount] = tx.amount
                    it[Transactions.accountId] = accountId
                    it[Transactions.transactionAmount] = tx.transactionAmount
                    it[Transactions.transactionCurrency] = tx.transactionCurrency
                    it[Transactions.bankId] = bankId
                    it[Transactions.paymentMethod] = tx.paymentMethod
                    it[Transactions.description] = tx.description
                    it[Transactions.merchantId] = merchantId
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
            append(tx.amount)
            append(tx.transactionAmount ?: "")
            append(tx.transactionCurrency ?: "")
            append(tx.bankName ?: "")
            append(tx.paymentMethod ?: "")
            append(tx.description)
            // Merchant data is not included in hash as it may change
            // but the transaction itself remains the same
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(hashData.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

data class TransactionData(
    val date: java.time.LocalDate,

    // Amount in account currency
    val amount: BigDecimal,
    val currency: String,

    // Amount in transaction currency (optional)
    val transactionAmount: BigDecimal?,
    val transactionCurrency: String?,

    // Merchant details (for creating/finding merchant)
    val merchantName: String?,
    val merchantLocation: String?,
    val mccCode: String?,

    // Payment details
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
