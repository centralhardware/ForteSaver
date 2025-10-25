package database

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

data class MerchantData(
    val id: Int,
    val name: String,
    val location: String?,
    val mccCode: String?,
    val categoryId: Int?,
    val needsCategorization: Boolean
)

object MerchantRepository {
    private val logger = LoggerFactory.getLogger(MerchantRepository::class.java)

    /**
     * Find or create a merchant based on name and location.
     * If merchant doesn't exist, creates it with needs_categorization=true and null category.
     * Returns the merchant ID.
     */
    suspend fun findOrCreateMerchant(
        name: String?,
        location: String?,
        mccCode: String?
    ): Int? = newSuspendedTransaction(Dispatchers.IO) {
        // If no merchant name, we can't create/find a merchant
        if (name.isNullOrBlank()) {
            return@newSuspendedTransaction null
        }

        // Normalize location (null if blank)
        val normalizedLocation = location?.takeIf { it.isNotBlank() }

        // Try to find existing merchant by name and location
        val existingMerchant = Merchants
            .selectAll()
            .where { (Merchants.name eq name) and (Merchants.location eq normalizedLocation) }
            .singleOrNull()

        if (existingMerchant != null) {
            // Merchant exists, return its ID
            existingMerchant[Merchants.id].value
        } else {
            // Merchant doesn't exist, create new one
            val merchantId = Merchants.insert {
                it[Merchants.name] = name
                it[Merchants.location] = normalizedLocation
                it[Merchants.mccCode] = mccCode
                it[Merchants.categoryId] = null  // No category assigned yet
                it[Merchants.needsCategorization] = true  // Needs manual categorization
                it[Merchants.createdAt] = LocalDateTime.now()
                it[Merchants.updatedAt] = LocalDateTime.now()
            } get Merchants.id

            logger.info("Created new merchant: '$name' (location: $normalizedLocation, mcc: $mccCode) - needs categorization")

            merchantId.value
        }
    }

    /**
     * Get merchant by ID
     */
    suspend fun getMerchant(merchantId: Int): MerchantData? = newSuspendedTransaction(Dispatchers.IO) {
        Merchants
            .selectAll()
            .where { Merchants.id eq merchantId }
            .singleOrNull()
            ?.let { row ->
                MerchantData(
                    id = row[Merchants.id].value,
                    name = row[Merchants.name],
                    location = row[Merchants.location],
                    mccCode = row[Merchants.mccCode],
                    categoryId = row[Merchants.categoryId]?.value,
                    needsCategorization = row[Merchants.needsCategorization]
                )
            }
    }

    /**
     * Get all merchants that need categorization
     */
    suspend fun getMerchantsNeedingCategorization(): List<MerchantData> = newSuspendedTransaction(Dispatchers.IO) {
        Merchants
            .selectAll()
            .where { Merchants.needsCategorization eq true }
            .orderBy(Merchants.createdAt to SortOrder.DESC)
            .map { row ->
                MerchantData(
                    id = row[Merchants.id].value,
                    name = row[Merchants.name],
                    location = row[Merchants.location],
                    mccCode = row[Merchants.mccCode],
                    categoryId = row[Merchants.categoryId]?.value,
                    needsCategorization = row[Merchants.needsCategorization]
                )
            }
    }

    /**
     * Update merchant category
     */
    suspend fun updateMerchantCategory(
        merchantId: Int,
        categoryId: Int
    ): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val updated = Merchants.update(where = { Merchants.id eq merchantId }) {
            it[Merchants.categoryId] = categoryId
            it[Merchants.needsCategorization] = false
            it[Merchants.updatedAt] = LocalDateTime.now()
        }

        if (updated > 0) {
            logger.info("Updated merchant $merchantId with category $categoryId")
            true
        } else {
            logger.warn("Failed to update merchant $merchantId - not found")
            false
        }
    }

    /**
     * Get all categories
     */
    suspend fun getAllCategories(): List<Pair<Int, String>> = newSuspendedTransaction(Dispatchers.IO) {
        Categories
            .selectAll()
            .orderBy(Categories.name to SortOrder.ASC)
            .map { row ->
                row[Categories.id].value to row[Categories.name]
            }
    }
}
