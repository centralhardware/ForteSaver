import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

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
    @Serializable(with = LocalDateTimeSerializer::class)
    val dateTime: LocalDateTime,
    val description: String,
    val debit: Double?,
    val credit: Double?,
    val balance: Double,
    val reference: String?
)
