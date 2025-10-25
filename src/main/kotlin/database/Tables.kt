package database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object Transactions : IntIdTable("transactions") {
    val transactionDate = date("transaction_date")
    val transactionType = varchar("transaction_type", 50)

    // Amount in account currency
    val amount = decimal("amount", 15, 2)
    val currency = varchar("currency", 10)

    // Amount in transaction currency (optional)
    val transactionAmount = decimal("transaction_amount", 15, 2).nullable()
    val transactionCurrency = varchar("transaction_currency", 10).nullable()

    // Merchant/counterparty details
    val merchantName = text("merchant_name").nullable()
    val merchantLocation = text("merchant_location").nullable()

    // Payment details
    val mccCode = varchar("mcc_code", 10).nullable()
    val bankName = varchar("bank_name", 255).nullable()
    val paymentMethod = varchar("payment_method", 50).nullable()

    // Full description
    val description = text("description").nullable()

    // Account information
    val accountNumber = varchar("account_number", 50).nullable()

    // Deduplication - daily sequence (order within same day)
    // Assumption: Bank sorts transactions by time within each day
    val dailySequence = integer("daily_sequence")

    // Keep transaction hash for analytics (no longer unique)
    val transactionHash = varchar("transaction_hash", 64).index()

    val createdAt = datetime("created_at")

    init {
        // Unique constraint: same currency + same daily_sequence + same hash = duplicate
        // This handles multiple currency accounts where transaction order may differ
        // Hash ensures the same transaction is not imported twice
        uniqueIndex("unique_currency_sequence_hash", currency, dailySequence, transactionHash)
    }
}
