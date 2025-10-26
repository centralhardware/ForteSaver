package services

import database.Categories
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MerchantCategorizationServiceTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setupDatabase() {
            // Connect to in-memory H2 database for testing
            Database.connect(
                url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                driver = "org.h2.Driver"
            )
        }
    }

    @BeforeEach
    fun setup() {
        // Create tables and add test categories
        transaction {
            SchemaUtils.create(Categories)

            // Add some test categories
            Categories.insert {
                it[name] = "Groceries"
                it[description] = "Test category"
                it[createdAt] = java.time.LocalDateTime.now()
            }
            Categories.insert {
                it[name] = "Restaurants"
                it[description] = "Test category"
                it[createdAt] = java.time.LocalDateTime.now()
            }
            Categories.insert {
                it[name] = "Transportation"
                it[description] = "Test category"
                it[createdAt] = java.time.LocalDateTime.now()
            }
            Categories.insert {
                it[name] = "Shopping"
                it[description] = "Test category"
                it[createdAt] = java.time.LocalDateTime.now()
            }
        }
    }

    @AfterEach
    fun teardown() {
        transaction {
            SchemaUtils.drop(Categories)
        }
    }
    
    @Test
    fun `test categorization by MCC code - grocery store`() = runBlocking {
        val categoryId = MerchantCategorizationService.autoCategorize(
            merchantName = "Unknown Store",
            mccCode = "5411" // Grocery Stores
        )
        
        assertNotNull(categoryId, "Should categorize by MCC code")
        
        val categoryName = transaction {
            Categories.selectAll()
                .where { Categories.id eq categoryId }
                .single()[Categories.name]
        }
        
        assertEquals("Groceries", categoryName)
    }
    
    @Test
    fun `test categorization by MCC code - restaurant`() = runBlocking {
        val categoryId = MerchantCategorizationService.autoCategorize(
            merchantName = "Unknown Restaurant",
            mccCode = "5812" // Restaurants
        )
        
        assertNotNull(categoryId)
        
        val categoryName = transaction {
            Categories.selectAll()
                .where { Categories.id eq categoryId }
                .single()[Categories.name]
        }
        
        assertEquals("Restaurants", categoryName)
    }
    
    @Test
    fun `test categorization by name - Russian grocery store`() = runBlocking {
        val categoryId = MerchantCategorizationService.autoCategorize(
            merchantName = "ПЯТЁРОЧКА №123",
            mccCode = null
        )
        
        assertNotNull(categoryId, "Should categorize Russian grocery store")
        
        val categoryName = transaction {
            Categories.selectAll()
                .where { Categories.id eq categoryId }
                .single()[Categories.name]
        }
        
        assertEquals("Groceries", categoryName)
    }
    
    @Test
    fun `test categorization by name - McDonald's`() = runBlocking {
        val categoryId = MerchantCategorizationService.autoCategorize(
            merchantName = "McDonald's",
            mccCode = null
        )
        
        assertNotNull(categoryId)
        
        val categoryName = transaction {
            Categories.selectAll()
                .where { Categories.id eq categoryId }
                .single()[Categories.name]
        }
        
        assertEquals("Restaurants", categoryName)
    }
    
    @Test
    fun `test categorization by name - Yandex Taxi`() = runBlocking {
        val categoryId = MerchantCategorizationService.autoCategorize(
            merchantName = "Yandex.Taxi",
            mccCode = null
        )
        
        assertNotNull(categoryId)
        
        val categoryName = transaction {
            Categories.selectAll()
                .where { Categories.id eq categoryId }
                .single()[Categories.name]
        }
        
        assertEquals("Transportation", categoryName)
    }
    
    @Test
    fun `test categorization by name - Wildberries shopping`() = runBlocking {
        val categoryId = MerchantCategorizationService.autoCategorize(
            merchantName = "WILDBERRIES",
            mccCode = null
        )
        
        assertNotNull(categoryId)
        
        val categoryName = transaction {
            Categories.selectAll()
                .where { Categories.id eq categoryId }
                .single()[Categories.name]
        }
        
        assertEquals("Shopping", categoryName)
    }
    
    @Test
    fun `test no categorization for unknown merchant`() = runBlocking {
        val categoryId = MerchantCategorizationService.autoCategorize(
            merchantName = "Some Random Merchant XYZ",
            mccCode = null
        )
        
        assertNull(categoryId, "Should not categorize unknown merchant")
    }
    
    @Test
    fun `test MCC code takes priority over name`() = runBlocking {
        // Even though name might suggest "taxi", MCC code says "restaurant"
        val categoryId = MerchantCategorizationService.autoCategorize(
            merchantName = "Taxi Pizza",
            mccCode = "5812" // Restaurant MCC
        )
        
        assertNotNull(categoryId)
        
        val categoryName = transaction {
            Categories.selectAll()
                .where { Categories.id eq categoryId }
                .single()[Categories.name]
        }
        
        assertEquals("Restaurants", categoryName, "MCC code should take priority")
    }
}
