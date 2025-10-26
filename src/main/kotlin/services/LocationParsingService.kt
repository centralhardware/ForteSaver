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
     * Parse location string from bank statement.
     *
     * Supported formats:
     * - "KUALA LUMPUR,MY" -> city="KUALA LUMPUR", country="MY"
     * - "BUDVA ME" -> city="BUDVA", country="ME"
     * - "PODGORICA ME" -> city="PODGORICA", country="ME"
     * - "LONDON GB" -> city="LONDON", country="GB"
     * - "QUILL CITY MALL,KUALA LUMPUR,MY" -> city="KUALA LUMPUR", country="MY"
     * - "SOUTH JAKARTA ID" -> city="SOUTH JAKARTA", country="ID"
     * - "BADUNG KAB. ID" -> city="BADUNG KAB.", country="ID"
     * - "HO CHI MINH CITY VN" -> city="HO CHI MINH CITY", country="VN"
     */
    fun parseLocation(location: String?): ParsedLocation {
        if (location.isNullOrBlank()) {
            return ParsedLocation(null, null)
        }

        val cleaned = location.trim().uppercase()

        // Try comma-separated format first (most reliable)
        // Format: "MERCHANT,CITY,COUNTRY" or "CITY,COUNTRY"
        if (cleaned.contains(',')) {
            return parseCommaFormat(cleaned)
        }

        // Try space-separated format: "CITY PARTS... COUNTRY"
        return parseSpaceFormat(cleaned)
    }

    /**
     * Parse comma-separated location format.
     * Format: "PART1,PART2,...,PARTN" where last part is country code.
     */
    private fun parseCommaFormat(location: String): ParsedLocation {
        val parts = location.split(',').map { it.trim() }.filter { it.isNotEmpty() }

        if (parts.isEmpty()) {
            return ParsedLocation(null, null)
        }

        // Last part should be 2-letter country code
        val lastPart = parts.last()
        if (isCountryCode(lastPart)) {
            val countryCode = lastPart

            // Second-to-last part is likely the city
            val city = if (parts.size >= 2) {
                parts[parts.size - 2]
            } else {
                null
            }

            return ParsedLocation(countryCode, city)
        }

        // No valid country code found
        return ParsedLocation(null, null)
    }

    /**
     * Parse space-separated location format.
     * Format: "CITY PARTS... COUNTRY_CODE"
     *
     * This is more complex because we need to detect multi-word cities.
     */
    private fun parseSpaceFormat(location: String): ParsedLocation {
        val words = location.split(Regex("\\s+")).filter { it.isNotEmpty() }

        if (words.isEmpty()) {
            return ParsedLocation(null, null)
        }

        // Check if last word is a country code
        val lastWord = words.last()
        if (!isCountryCode(lastWord)) {
            // No country code found - try to extract anyway
            // Maybe the location is just a city name
            return ParsedLocation(null, location)
        }

        val countryCode = lastWord
        val wordsBeforeCountry = words.dropLast(1)

        if (wordsBeforeCountry.isEmpty()) {
            return ParsedLocation(countryCode, null)
        }

        // Determine how many words belong to the city name
        // Pass country code for GeoNames validation
        val cityWordCount = determineCityWordCount(wordsBeforeCountry, countryCode)

        // Extract city (take last N words before country code)
        val cityInput = wordsBeforeCountry
            .takeLast(cityWordCount)
            .joinToString(" ")

        // Get canonical city name from database (handles fuzzy matching)
        val city = if (cityInput.isNotBlank()) {
            GeoNamesCityDatabase.findCityMatch(cityInput, countryCode) ?: cityInput
        } else {
            null
        }

        return ParsedLocation(
            countryCode,
            city
        )
    }

    /**
     * Determine how many words from the end belong to the city name.
     * This uses heuristics to handle multi-word city names.
     *
     * @param words Words before the country code
     * @param countryCode Country code for GeoNames validation (optional)
     */
    private fun determineCityWordCount(words: List<String>, countryCode: String? = null): Int {
        if (words.isEmpty()) return 0
        if (words.size == 1) {
            // Single word - validate with GeoNamesCityDatabase if country code available
            if (countryCode != null) {
                val cityName = words[0]
                if (GeoNamesCityDatabase.isCityInCountry(cityName, countryCode)) {
                    return 1
                }
                // Not a valid city - return 0
                return 0
            }
            return 1
        }

        // Pattern 1: Check for administrative indicators (KAB., KOTA, CITY, AIRPORT, etc.)
        // These are part of the city name - take all words
        val lastWord = words.last()
        if (lastWord in CITY_INDICATORS || lastWord.endsWith(".")) {
            // The indicator is at the end - take all words before country code
            return words.size
        }

        // Pattern 2: Try to find the longest valid city name using GeoNamesCityDatabase
        // Start from longest possible (3 words) down to shortest (1 word)
        if (countryCode != null) {
            // Try 3-word cities first
            if (words.size >= 3) {
                val threeWordCity = words.takeLast(3).joinToString(" ")
                if (GeoNamesCityDatabase.isCityInCountry(threeWordCity, countryCode)) {
                    return 3
                }
            }

            // Try 2-word cities
            if (words.size >= 2) {
                val twoWordCity = words.takeLast(2).joinToString(" ")
                if (GeoNamesCityDatabase.isCityInCountry(twoWordCity, countryCode)) {
                    return 2
                }
            }

            // Try 1-word city
            val oneWordCity = words.last()
            if (GeoNamesCityDatabase.isCityInCountry(oneWordCity, countryCode)) {
                return 1
            }

            // No valid city found via GeoNamesCityDatabase - return 0
            return 0
        }

        // Fallback: Use heuristics if no country code provided
        // Pattern 3: Three words that might be a city (e.g., "HO CHI MINH")
        if (words.size >= 3) {
            val lastThree = words.takeLast(3)
            // Check for specific patterns
            if (lastThree.all { it.length <= 10 && it.all { c -> c.isLetter() } }) {
                // Could be a 3-word city
                // Check if first word is a direction or common prefix
                val firstWord = lastThree[0]
                if (firstWord in setOf("NORTH", "SOUTH", "EAST", "WEST", "NEW", "SAN", "LOS", "HO")) {
                    return 3
                }
                // Check for Vietnamese/Asian city patterns (short words)
                if (lastThree.all { it.length <= 5 }) {
                    return 3
                }
            }
        }

        // Pattern 4: Two capitalized words (e.g., "SOUTH JAKARTA", "KUALA LUMPUR")
        if (words.size >= 2) {
            val lastTwo = words.takeLast(2)
            if (lastTwo.all { it[0].isUpperCase() || it.all { c -> c.isLetter() } }) {
                // Check if they look like a city name (not merchant name)
                // Heuristic: If both words are relatively short (< 15 chars), likely a city
                if (lastTwo.all { it.length < 15 }) {
                    return 2
                }
            }
        }

        // Default: assume single-word city
        return 1
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
