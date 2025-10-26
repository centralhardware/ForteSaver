package services

/**
 * Service for parsing and normalizing merchant location strings.
 * Handles various location formats from bank statements.
 */
object LocationParsingService {

    data class ParsedLocation(
        val countryCode: String?,
        val city: String?
    )

    // Common 2-letter country codes to help identify countries
    private val KNOWN_COUNTRY_CODES = setOf(
        "AF", "AL", "DZ", "AD", "AO", "AG", "AR", "AM", "AU", "AT", "AZ", "BS", "BH", "BD", "BB", "BY", "BE", "BZ",
        "BJ", "BT", "BO", "BA", "BW", "BR", "BN", "BG", "BF", "BI", "KH", "CM", "CA", "CV", "CF", "TD", "CL", "CN",
        "CO", "KM", "CG", "CD", "CR", "CI", "HR", "CU", "CY", "CZ", "DK", "DJ", "DM", "DO", "EC", "EG", "SV", "GQ",
        "ER", "EE", "ET", "FJ", "FI", "FR", "GA", "GM", "GE", "DE", "GH", "GR", "GD", "GT", "GN", "GW", "GY", "HT",
        "HN", "HU", "IS", "IN", "ID", "IR", "IQ", "IE", "IL", "IT", "JM", "JP", "JO", "KZ", "KE", "KI", "KP", "KR",
        "KW", "KG", "LA", "LV", "LB", "LS", "LR", "LY", "LI", "LT", "LU", "MK", "MG", "MW", "MY", "MV", "ML", "MT",
        "MH", "MR", "MU", "MX", "FM", "MD", "MC", "MN", "ME", "MA", "MZ", "MM", "NA", "NR", "NP", "NL", "NZ", "NI",
        "NE", "NG", "NO", "OM", "PK", "PW", "PS", "PA", "PG", "PY", "PE", "PH", "PL", "PT", "QA", "RO", "RU", "RW",
        "KN", "LC", "VC", "WS", "SM", "ST", "SA", "SN", "RS", "SC", "SL", "SG", "SK", "SI", "SB", "SO", "ZA", "SS",
        "ES", "LK", "SD", "SR", "SZ", "SE", "CH", "SY", "TJ", "TZ", "TH", "TL", "TG", "TO", "TT", "TN", "TR", "TM",
        "TV", "UG", "UA", "AE", "GB", "US", "UY", "UZ", "VU", "VA", "VE", "VN", "YE", "ZM", "ZW"
    )

    // Common words that appear in city names or administrative divisions
    private val CITY_INDICATORS = setOf(
        "CITY", "MALL", "AIRPORT", "PORT", "TOWN", "VILLAGE", "DISTRICT",
        "KAB.", "KABUPATEN", "KOTA", "PROVINCE", "DISTRICT", "REGENCY"
    )

    /**
     * Parse location from merchant name string.
     *
     * New logic:
     * - Country is ALWAYS the last word (must be valid 2-letter country code)
     * - City is the second from the end, validated against GeoNames database
     * - If city is multi-word, we check multiple words starting from second-to-last position
     *
     * Examples:
     * - "SOME MERCHANT PODGORICA ME" -> city="PODGORICA", country="ME"
     * - "STORE NAME KUALA LUMPUR MY" -> city="KUALA LUMPUR", country="MY"
     * - "WWW.SITE.COM SARAJEVO BA" -> city="SARAJEVO", country="BA"
     */
    fun parseLocation(merchantName: String?): ParsedLocation {
        if (merchantName.isNullOrBlank()) {
            return ParsedLocation(null, null)
        }

        val cleaned = merchantName.trim().uppercase()
        val words = cleaned.split(Regex("\\s+")).filter { it.isNotEmpty() }

        if (words.isEmpty()) {
            return ParsedLocation(null, null)
        }

        // Country is ALWAYS the last word
        val lastWord = words.last()
        
        // Check if it's a valid country code
        if (!isCountryCode(lastWord)) {
            // No valid country code at the end
            return ParsedLocation(null, null)
        }

        val countryCode = lastWord
        val wordsBeforeCountry = words.dropLast(1)

        if (wordsBeforeCountry.isEmpty()) {
            // Only country code, no city
            return ParsedLocation(countryCode, null)
        }

        // Try to find city starting from second-to-last position
        // Check 1-word, 2-word, 3-word cities
        val city = findCityInWords(wordsBeforeCountry, countryCode)

        return ParsedLocation(countryCode, city)
    }

    /**
     * Find city name from words before the country code.
     * Tries to match 1-word, 2-word, or 3-word cities against GeoNames database.
     * Starts from the end (second-to-last position) and works backwards.
     */
    private fun findCityInWords(words: List<String>, countryCode: String): String? {
        if (words.isEmpty()) return null

        // Try 3-word cities (starting from the last 3 words)
        if (words.size >= 3) {
            val threeWordCity = words.takeLast(3).joinToString(" ")
            if (GeoNamesCityDatabase.isCityInCountry(threeWordCity, countryCode)) {
                return GeoNamesCityDatabase.findCityMatch(threeWordCity, countryCode)
            }
        }

        // Try 2-word cities (starting from the last 2 words)
        if (words.size >= 2) {
            val twoWordCity = words.takeLast(2).joinToString(" ")
            if (GeoNamesCityDatabase.isCityInCountry(twoWordCity, countryCode)) {
                return GeoNamesCityDatabase.findCityMatch(twoWordCity, countryCode)
            }
        }

        // Try 1-word city (the last word before country)
        val oneWordCity = words.last()
        if (GeoNamesCityDatabase.isCityInCountry(oneWordCity, countryCode)) {
            return GeoNamesCityDatabase.findCityMatch(oneWordCity, countryCode)
        }

        // No valid city found in database
        return null
    }

    /**
     * Check if a string is a valid 2-letter country code.
     */
    private fun isCountryCode(code: String): Boolean {
        if (code.length != 2) return false
        if (!code.all { it.isLetter() }) return false

        // Check against known country codes
        return code.uppercase() in KNOWN_COUNTRY_CODES
    }

    /**
     * Normalize city name for consistent comparison.
     * Removes common prefixes/suffixes and standardizes format.
     */
    fun normalizeCity(city: String?): String? {
        if (city.isNullOrBlank()) return null
        
        val normalized = city.trim().uppercase()
        
        // Remove common merchant-specific suffixes
        val cleaned = normalized
            .replace(Regex("\\s+(MALL|AIRPORT|STATION|CENTER|CENTRE)$"), "")
            .trim()
        
        return if (cleaned.isNotBlank()) cleaned else null
    }

    /**
     * Check if country code is likely the user's home country.
     * Currently assumes Montenegro (ME) as home country.
     * TODO: Make this configurable per user/account.
     */
    fun isHomeCountry(countryCode: String?): Boolean {
        if (countryCode.isNullOrBlank()) return true // Unknown = assume home
        
        // TODO: Load from user settings or account configuration
        val homeCountries = setOf("ME", "RS") // Montenegro and Serbia as example
        
        return countryCode.uppercase() in homeCountries
    }

    /**
     * Calculate similarity between two location strings (0.0 to 1.0).
     * Used for grouping similar locations in trip detection.
     */
    fun locationSimilarity(loc1: ParsedLocation, loc2: ParsedLocation): Double {
        // Both null - similar
        if (loc1.countryCode == null && loc2.countryCode == null) return 1.0
        
        // One null - different
        if (loc1.countryCode == null || loc2.countryCode == null) return 0.0
        
        // Different countries - completely different
        if (loc1.countryCode != loc2.countryCode) return 0.0
        
        // Same country, both cities unknown - similar
        if (loc1.city == null && loc2.city == null) return 0.8
        
        // Same country, one city unknown - somewhat similar
        if (loc1.city == null || loc2.city == null) return 0.6
        
        // Same country and city - identical
        if (normalizeCity(loc1.city) == normalizeCity(loc2.city)) return 1.0
        
        // Same country, different cities - different trips
        return 0.0
    }
}
