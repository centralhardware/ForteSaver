import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class BankStatement(
    val accountHolder: String,
    val accountNumber: String,
    val currency: String,
    val period: DatePeriod,
    val openingBalance: Double,
    val closingBalance: Double,
    val transactions: List<Transaction>
)

@Serializable
data class DatePeriod(
    @Serializable(with = LocalDateSerializer::class)
    val from: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val to: LocalDate
)

@Serializable
data class Transaction(
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,

    // Amount in account currency
    val amount: Double,
    val currency: String,

    // Amount in transaction currency (for foreign purchases)
    val transactionAmount: Double?,
    val transactionCurrency: String?,

    // Merchant details
    val merchantName: String?,
    val merchantLocation: String?,

    // Payment details
    val mccCode: String?,
    val bankName: String?,
    val paymentMethod: String?,

    // Full description
    val description: String
)
