package services

import database.Categories
import database.MerchantRepository
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory

/**
 * Service for automatic categorization of merchants based on various rules:
 * 1. MCC codes (Merchant Category Codes)
 * 2. Keywords in merchant name
 * 3. Merchant location patterns
 */
object MerchantCategorizationService {
    private val logger = LoggerFactory.getLogger(MerchantCategorizationService::class.java)

    /**
     * Attempt to automatically categorize a merchant based on available data.
     * Returns the category ID if successful, null otherwise.
     */
    suspend fun autoCategorize(
        merchantName: String,
        location: String?,
        mccCode: String?
    ): Int? {
        // Try MCC code first (most reliable)
        mccCode?.let { code ->
            categorizeByCategoryCode(code)?.let { categoryId ->
                logger.debug("Categorized '$merchantName' by MCC code $code")
                return categoryId
            }
        }

        // Try merchant name patterns
        categorizeByName(merchantName)?.let { categoryId ->
            logger.debug("Categorized '$merchantName' by name pattern")
            return categoryId
        }

        // Could not auto-categorize
        logger.debug("Could not auto-categorize merchant: $merchantName")
        return null
    }

    /**
     * Categorize based on MCC (Merchant Category Code)
     * MCC codes are standardized 4-digit codes assigned to merchants by payment networks
     */
    private suspend fun categorizeByCategoryCode(mccCode: String): Int? {
        val categoryName = mccToCategoryMap[mccCode] ?: return null
        return getCategoryIdByName(categoryName)
    }

    /**
     * Categorize based on merchant name keywords
     */
    private suspend fun categorizeByName(merchantName: String): Int? {
        val normalizedName = merchantName.lowercase().trim()

        // Check each category's keywords
        for ((categoryName, keywords) in categoryKeywords) {
            if (keywords.any { keyword -> normalizedName.contains(keyword.lowercase()) }) {
                return getCategoryIdByName(categoryName)
            }
        }

        return null
    }

    /**
     * Get category ID by name from database
     */
    private suspend fun getCategoryIdByName(categoryName: String): Int? =
        newSuspendedTransaction(Dispatchers.IO) {
            Categories
                .selectAll()
                .where { Categories.name eq categoryName }
                .singleOrNull()
                ?.get(Categories.id)?.value
        }

    // MCC code to category mapping
    // Based on standard MCC codes: https://www.merchantsavvy.co.uk/mcc-codes/
    private val mccToCategoryMap = mapOf(
        // Groceries
        "5411" to "Groceries", // Grocery Stores, Supermarkets
        "5422" to "Groceries", // Freezer and Locker Meat Provisioners
        "5441" to "Groceries", // Candy, Nut, and Confectionery Stores
        "5451" to "Groceries", // Dairy Products Stores
        "5462" to "Groceries", // Bakeries

        // Restaurants
        "5811" to "Restaurants", // Caterers
        "5812" to "Restaurants", // Eating Places, Restaurants
        "5813" to "Restaurants", // Drinking Places (Alcoholic Beverages), Bars, Taverns
        "5814" to "Restaurants", // Fast Food Restaurants

        // Transportation
        "4111" to "Transportation", // Local/Suburban Commuter Passenger Transportation
        "4121" to "Transportation", // Taxicabs and Limousines
        "4131" to "Transportation", // Bus Lines
        "5541" to "Transportation", // Service Stations (with or without ancillary services)
        "5542" to "Transportation", // Automated Fuel Dispensers
        "5172" to "Transportation", // Petroleum and Petroleum Products
        "4789" to "Transportation", // Transportation Services

        // Shopping
        "5311" to "Shopping", // Department Stores
        "5651" to "Shopping", // Family Clothing Stores
        "5661" to "Shopping", // Shoe Stores
        "5732" to "Shopping", // Electronics Stores
        "5734" to "Shopping", // Computer Software Stores
        "5735" to "Shopping", // Record Stores
        "5941" to "Shopping", // Sporting Goods Stores
        "5942" to "Shopping", // Book Stores
        "5943" to "Shopping", // Stationery Stores
        "5945" to "Shopping", // Hobby, Toy, and Game Shops
        "5977" to "Shopping", // Cosmetics Stores

        // Entertainment
        "5815" to "Entertainment", // Digital Goods Media – Books, Movies, Music
        "5816" to "Entertainment", // Digital Goods – Games
        "5817" to "Entertainment", // Digital Goods – Applications
        "5818" to "Entertainment", // Digital Goods – Large Digital Goods Merchant
        "5932" to "Entertainment", // Antique Shops
        "7832" to "Entertainment", // Motion Picture Theaters
        "7841" to "Entertainment", // Video Entertainment Rental Stores
        "7911" to "Entertainment", // Dance Halls, Studios, and Schools
        "7922" to "Entertainment", // Theatrical Producers and Ticket Agencies
        "7929" to "Entertainment", // Bands, Orchestras, and Miscellaneous Entertainers
        "7932" to "Entertainment", // Billiard and Pool Establishments
        "7933" to "Entertainment", // Bowling Alleys
        "7991" to "Entertainment", // Tourist Attractions and Exhibits
        "7992" to "Entertainment", // Golf Courses – Public
        "7993" to "Entertainment", // Video Amusement Game Supplies
        "7994" to "Entertainment", // Video Game Arcades/Establishments
        "7995" to "Entertainment", // Betting (including Lottery Tickets, Casino Gaming Chips)
        "7996" to "Entertainment", // Amusement Parks, Carnivals, Circuses
        "7998" to "Entertainment", // Aquariums, Seaquariums, Dolphinariums

        // Healthcare
        "5912" to "Healthcare", // Drug Stores and Pharmacies
        "5976" to "Healthcare", // Orthopedic Goods – Prosthetic Devices
        "8011" to "Healthcare", // Doctors
        "8021" to "Healthcare", // Dentists and Orthodontists
        "8031" to "Healthcare", // Osteopaths
        "8041" to "Healthcare", // Chiropractors
        "8042" to "Healthcare", // Optometrists, Ophthalmologist
        "8043" to "Healthcare", // Opticians, Optical Goods, and Eyeglasses
        "8049" to "Healthcare", // Podiatrists, Chiropodists
        "8050" to "Healthcare", // Nursing/Personal Care Facilities
        "8062" to "Healthcare", // Hospitals
        "8071" to "Healthcare", // Medical and Dental Laboratories

        // Utilities
        "4814" to "Utilities", // Telecommunication Services
        "4816" to "Utilities", // Computer Network Services
        "4899" to "Utilities", // Cable, Satellite, and Other Pay Television
        "4900" to "Utilities", // Electric, Gas, Sanitary and Water Utilities

        // Travel
        "3000" to "Travel", // Airlines
        "3001" to "Travel", // American Airlines
        "3351" to "Travel", // Hilton Hotels
        "3501" to "Travel", // Holiday Inns
        "4511" to "Travel", // Airlines, Air Carriers
        "7011" to "Travel", // Hotels, Motels, and Resorts
        "7512" to "Travel", // Car Rental Agencies
        "7513" to "Travel", // Truck and Utility Trailer Rentals
        "7519" to "Travel", // Recreational Vehicle Rentals

        // Education
        "5942" to "Education", // Book Stores
        "8211" to "Education", // Elementary, Secondary Schools
        "8220" to "Education", // Colleges, Universities
        "8241" to "Education", // Correspondence Schools
        "8244" to "Education", // Business/Secretarial Schools
        "8249" to "Education", // Vocational/Trade Schools
        "8299" to "Education", // Educational Services

        // Sports
        "5655" to "Sports", // Sports and Riding Apparel Stores
        "5941" to "Sports", // Sporting Goods Stores
        "7012" to "Sports", // Timeshares
        "7997" to "Sports", // Membership Clubs (Sports, Recreation, Athletic)
        "7999" to "Sports", // Recreation Services

        // Home
        "5021" to "Home", // Office and Commercial Furniture
        "5039" to "Home", // Construction Materials
        "5046" to "Home", // Commercial Equipment
        "5211" to "Home", // Lumber, Building Materials Stores
        "5231" to "Home", // Glass, Paint, and Wallpaper Stores
        "5251" to "Home", // Hardware Stores
        "5712" to "Home", // Furniture, Home Furnishings, and Equipment Stores
        "5713" to "Home", // Floor Covering Stores
        "5714" to "Home", // Drapery, Window Covering, and Upholstery Stores
        "5718" to "Home", // Fireplaces, Fireplace Screens, and Accessories Stores

        // Beauty
        "5977" to "Beauty", // Cosmetic Stores
        "7230" to "Beauty", // Barber and Beauty Shops
        "7297" to "Beauty", // Massage Parlors
        "7298" to "Beauty", // Health and Beauty Spas

        // Pets
        "5995" to "Pets", // Pet Shops, Pet Food, and Supplies
        "0742" to "Pets", // Veterinary Services

        // Transfers
        "6012" to "Transfers", // Financial Institutions
        "6011" to "Transfers", // Automated Cash Disburse

        // Cash
        "6010" to "Cash", // Manual Cash Disbursements
        "6011" to "Cash", // Automated Cash Disburse

        // Fees
        "9311" to "Fees", // Tax Payments
        "9399" to "Fees" // Government Services
    )

    // Keyword-based category mapping
    // These are used when MCC code is not available
    private val categoryKeywords = mapOf(
        "Groceries" to listOf(
            "supermarket", "grocery", "market", "продукты", "магазин",
            "carrefour", "ашан", "пятёрочка", "перекрёсток", "магнит",
            "дикси", "лента", "окей", "metro"
        ),
        "Restaurants" to listOf(
            "restaurant", "cafe", "coffee", "pizza", "burger", "sushi",
            "ресторан", "кафе", "кофе", "bar", "pub", "мак",
            "mcdonald", "kfc", "subway", "dominos", "starbucks",
            "delivery", "доставка", "яндекс еда", "деливери"
        ),
        "Transportation" to listOf(
            "taxi", "uber", "yandex", "metro", "transport", "parking",
            "такси", "метро", "транспорт", "парковка", "azs", "газпром",
            "rosneft", "lukoil", "shell", "bp", "fuel", "бензин"
        ),
        "Shopping" to listOf(
            "shop", "store", "clothing", "shoes", "electronics", "fashion",
            "магазин", "одежда", "обувь", "zara", "h&m", "wildberries",
            "ozon", "lamoda", "aliexpress", "amazon", "ebay"
        ),
        "Entertainment" to listOf(
            "cinema", "movie", "theater", "game", "stream", "spotify",
            "netflix", "youtube", "кино", "theatre", "игр", "steam",
            "playstation", "xbox", "nintendo", "apple music"
        ),
        "Healthcare" to listOf(
            "pharmacy", "hospital", "clinic", "doctor", "medical", "health",
            "аптека", "клиника", "врач", "медиц", "здоровье", "36.6",
            "ригла", "здравсити"
        ),
        "Utilities" to listOf(
            "electric", "gas", "water", "internet", "telecom", "mobile",
            "электр", "газ", "вода", "интернет", "связь", "мобильн",
            "мтс", "билайн", "мегафон", "теле2", "ростелеком"
        ),
        "Travel" to listOf(
            "hotel", "flight", "airline", "booking", "airbnb", "travel",
            "отель", "гостиниц", "авиа", "самолет", "туриз", "тур",
            "s7", "аэрофлот", "pobeda"
        ),
        "Education" to listOf(
            "school", "university", "course", "education", "learning",
            "школ", "универ", "курс", "обучен", "образован", "udemy",
            "coursera", "skillbox"
        ),
        "Sports" to listOf(
            "gym", "fitness", "sport", "спорт", "фитнес", "тренаж",
            "world class", "gold's gym", "спортмастер"
        ),
        "Home" to listOf(
            "furniture", "ikea", "leroy", "obi", "мебель", "ремонт",
            "строй", "castorama", "hoff"
        ),
        "Beauty" to listOf(
            "salon", "beauty", "cosmetic", "spa", "салон", "красот",
            "косметик", "парикмахер", "sephora", "л'этуаль", "рив гош"
        ),
        "Pets" to listOf(
            "pet", "vet", "животн", "зоо", "ветеринар", "корм для",
            "четыре лапы", "бетховен"
        ),
        "Gifts" to listOf(
            "gift", "flower", "подарок", "цвет", "букет", "donation",
            "благотворительн"
        ),
        "Transfers" to listOf(
            "transfer", "перевод", "bank transfer", "p2p", "sbp",
            "система быстрых платежей"
        ),
        "Cash" to listOf(
            "atm", "cash", "банкомат", "наличные", "снятие"
        ),
        "Fees" to listOf(
            "fee", "commission", "комиссия", "плата", "tax", "налог"
        )
    )
}
