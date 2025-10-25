package database

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

data class BankData(
    val id: Int,
    val name: String
)

data class AccountData(
    val id: Int,
    val accountNumber: String,
    val currency: String
)

object AccountRepository {
    private val logger = LoggerFactory.getLogger(AccountRepository::class.java)

    /**
     * Find or create a bank by name.
     * Returns the bank ID.
     */
    suspend fun findOrCreateBank(bankName: String?): Int? = newSuspendedTransaction(Dispatchers.IO) {
        if (bankName.isNullOrBlank()) {
            return@newSuspendedTransaction null
        }

        // Try to find existing bank
        val existingBank = Banks
            .selectAll()
            .where { Banks.name eq bankName }
            .singleOrNull()

        if (existingBank != null) {
            existingBank[Banks.id].value
        } else {
            // Create new bank
            val bankId = Banks.insert {
                it[Banks.name] = bankName
                it[Banks.createdAt] = LocalDateTime.now()
            } get Banks.id

            logger.info("Created new bank: '$bankName'")
            bankId.value
        }
    }

    /**
     * Find or create an account by account number and currency.
     * All accounts are from Forte bank.
     * Returns the account ID.
     */
    suspend fun findOrCreateAccount(
        accountNumber: String?,
        currency: String
    ): Int = newSuspendedTransaction(Dispatchers.IO) {
        // If no account number provided, we can't create/find an account
        if (accountNumber.isNullOrBlank()) {
            throw IllegalArgumentException("Account number is required")
        }

        // Try to find existing account
        val existingAccount = Accounts
            .selectAll()
            .where { Accounts.accountNumber eq accountNumber }
            .singleOrNull()

        if (existingAccount != null) {
            existingAccount[Accounts.id].value
        } else {
            // Create new account
            val accountId = Accounts.insert {
                it[Accounts.accountNumber] = accountNumber
                it[Accounts.currency] = currency
                it[Accounts.createdAt] = LocalDateTime.now()
            } get Accounts.id

            logger.info("Created new account: '$accountNumber' ($currency)")
            accountId.value
        }
    }

    /**
     * Get account by ID
     */
    suspend fun getAccount(accountId: Int): AccountData? = newSuspendedTransaction(Dispatchers.IO) {
        Accounts
            .selectAll()
            .where { Accounts.id eq accountId }
            .singleOrNull()
            ?.let { row ->
                AccountData(
                    id = row[Accounts.id].value,
                    accountNumber = row[Accounts.accountNumber],
                    currency = row[Accounts.currency]
                )
            }
    }

    /**
     * Get all accounts
     */
    suspend fun getAllAccounts(): List<AccountData> = newSuspendedTransaction(Dispatchers.IO) {
        Accounts
            .selectAll()
            .orderBy(Accounts.accountNumber to SortOrder.ASC)
            .map { row ->
                AccountData(
                    id = row[Accounts.id].value,
                    accountNumber = row[Accounts.accountNumber],
                    currency = row[Accounts.currency]
                )
            }
    }

    /**
     * Get all banks
     */
    suspend fun getAllBanks(): List<BankData> = newSuspendedTransaction(Dispatchers.IO) {
        Banks
            .selectAll()
            .orderBy(Banks.name to SortOrder.ASC)
            .map { row ->
                BankData(
                    id = row[Banks.id].value,
                    name = row[Banks.name]
                )
            }
    }

}
