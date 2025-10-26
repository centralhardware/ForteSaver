package database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

// Reference tables for normalized data
object Banks : IntIdTable("banks") {
    val name = varchar("name", 255).uniqueIndex()
    val createdAt = datetime("created_at")
}

object Accounts : IntIdTable("accounts") {
    val accountNumber = varchar("account_number", 50).uniqueIndex()
    val currency = varchar("currency", 10)
    val createdAt = datetime("created_at")
}

object Categories : IntIdTable("categories") {
    val name = varchar("name", 100).uniqueIndex()
    val description = text("description").nullable()
    val createdAt = datetime("created_at")
}

object Merchants : IntIdTable("merchants") {
    val name = varchar("name", 500).uniqueIndex()
    val mccCode = varchar("mcc_code", 10).nullable()
    val categoryId = reference("category_id", Categories).nullable()
    val needsCategorization = bool("needs_categorization").default(true)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    init {
        index("idx_merchants_name", false, name)
        index("idx_merchants_category", false, categoryId)
        index("idx_merchants_needs_categorization", false, needsCategorization)
    }
}

object Transactions : IntIdTable("transactions") {
    val transactionDate = date("transaction_date")

    // Amount in account currency
    val amount = decimal("amount", 15, 2)
    val accountId = reference("account_id", Accounts)

    // Amount in transaction currency (optional)
    val transactionAmount = decimal("transaction_amount", 15, 2).nullable()
    val transactionCurrency = varchar("transaction_currency", 10).nullable()

    // Bank through which payment was made (merchant bank)
    val bankId = reference("bank_id", Banks).nullable()

    // Payment details
    val paymentMethod = varchar("payment_method", 50).nullable()

    // Full description
    val description = text("description").nullable()

    // Merchant reference
    val merchantId = reference("merchant_id", Merchants).nullable()

    // Deduplication - daily sequence (order within same day)
    // Assumption: Bank sorts transactions by time within each day
    val dailySequence = integer("daily_sequence")

    // Keep transaction hash for analytics (no longer unique)
    val transactionHash = varchar("transaction_hash", 64).index()

    val createdAt = datetime("created_at")

    init {
        // Unique constraint: same account + same daily_sequence + same hash = duplicate
        uniqueIndex("unique_account_sequence_hash", accountId, dailySequence, transactionHash)
    }
}
