package database

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import services.MerchantCategorizationService
import java.time.LocalDateTime

data class MerchantData(
    val id: Int,
    val name: String,
    val mccCode: String?,
    val categoryId: Int?,
    val needsCategorization: Boolean
)

object MerchantRepository {
    private val logger = LoggerFactory.getLogger(MerchantRepository::class.java)

    /**
     * Find or create a merchant based on name.
     * If merchant doesn't exist, creates it and attempts automatic categorization.
     * Name should contain the full merchant details string (merchant name + location).
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

        // Try to find existing merchant by name only
        val existingMerchant = Merchants
            .selectAll()
            .where { Merchants.name eq name }
            .singleOrNull()

        if (existingMerchant != null) {
            // Merchant exists, return its ID
            existingMerchant[Merchants.id].value
        } else {
            // Attempt automatic categorization
            val autoCategoryId = MerchantCategorizationService.autoCategorize(
                merchantName = name,
                mccCode = mccCode
            )

            val needsCategorization = autoCategoryId == null

            // Merchant doesn't exist, create new one
            val merchantId = Merchants.insert {
                it[Merchants.name] = name
                it[Merchants.mccCode] = mccCode
                it[Merchants.categoryId] = autoCategoryId
                it[Merchants.needsCategorization] = needsCategorization
                it[Merchants.createdAt] = LocalDateTime.now()
                it[Merchants.updatedAt] = LocalDateTime.now()
            } get Merchants.id

            if (autoCategoryId != null) {
                logger.info("Created new merchant: '$name' (mcc: $mccCode) - auto-categorized")
            } else {
                logger.info("Created new merchant: '$name' (mcc: $mccCode) - needs manual categorization")
            }

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

    /**
     * Attempt to automatically categorize all merchants that need categorization.
     * Returns the number of merchants successfully categorized.
     */
    suspend fun autoCategorizeExistingMerchants(): Int {
        val merchantsToProcess = getMerchantsNeedingCategorization()
        var categorizedCount = 0

        for (merchant in merchantsToProcess) {
            val categoryId = MerchantCategorizationService.autoCategorize(
                merchantName = merchant.name,
                mccCode = merchant.mccCode
            )

            if (categoryId != null) {
                val success = updateMerchantCategory(merchant.id, categoryId)
                if (success) {
                    categorizedCount++
                }
            }
        }

        logger.info("Auto-categorized $categorizedCount out of ${merchantsToProcess.size} merchants")
        return categorizedCount
    }
}
