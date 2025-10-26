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

    /**
     * Parse location string from bank statement.
     * 
     * Supported formats:
     * - "KUALA LUMPUR,MY" -> city="KUALA LUMPUR", country="MY"
     * - "BUDVA ME" -> city="BUDVA", country="ME"
     * - "PODGORICA ME" -> city="PODGORICA", country="ME"
     * - "LONDON GB" -> city="LONDON", country="GB"
     * - "QUILL CITY MALL,KUALA LUMPUR,MY" -> city="KUALA LUMPUR", country="MY"
     */
    fun parseLocation(location: String?): ParsedLocation {
        if (location.isNullOrBlank()) {
            return ParsedLocation(null, null)
        }

        val cleaned = location.trim().uppercase()

        // Try comma-separated format first (most specific)
        // Format: "MERCHANT,CITY,COUNTRY" or "MERCHANT,CITY,COUNTRY"
        if (cleaned.contains(',')) {
            val parts = cleaned.split(',').map { it.trim() }
            
            // Last part should be 2-letter country code
            val lastPart = parts.lastOrNull()
            if (lastPart != null && lastPart.length == 2 && lastPart.all { it.isLetter() }) {
                val countryCode = lastPart
                
                // Second-to-last part is city (if exists)
                val city = if (parts.size >= 2) {
                    parts[parts.size - 2]
                } else {
                    null
                }
                
                return ParsedLocation(countryCode, city)
            }
        }

        // Try space-separated format: "CITY COUNTRY"
        // Country code is typically last 2 characters if they're letters
        val parts = cleaned.split(Regex("\\s+"))
        
        if (parts.size >= 2) {
            val lastPart = parts.last()
            
            // Check if last part is 2-letter country code
            if (lastPart.length == 2 && lastPart.all { it.isLetter() }) {
                val countryCode = lastPart
                
                // Everything before country code is city
                val city = parts.dropLast(1).joinToString(" ")
                
                return ParsedLocation(countryCode, if (city.isNotBlank()) city else null)
            }
        }

        // Could not parse - return null
        return ParsedLocation(null, null)
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
