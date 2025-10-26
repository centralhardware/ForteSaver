package services

import org.junit.jupiter.api.Test

/**
 * Demonstration of the improved location parsing capabilities.
 * Run this to see how various location formats are parsed.
 */
class LocationParsingDemo {

    @Test
    fun `demonstrate location parsing improvements`() {
        println("=== Location Parsing Demonstration ===\n")

    val testCases = listOf(
        // Simple formats
        "BUDVA ME",
        "LONDON GB",
        "PODGORICA ME",

        // Comma-separated formats
        "KUALA LUMPUR,MY",
        "QUILL CITY MALL,KUALA LUMPUR,MY",

        // Two-word cities
        "SOUTH JAKARTA ID",
        "NORTH JAKARTA ID",
        "KUALA LUMPUR MY",
        "NEW YORK US",
        "SAN FRANCISCO US",
        "LOS ANGELES US",

        // Three-word cities
        "HO CHI MINH VN",
        "HO CHI MINH CITY VN",

        // Cities with administrative suffixes
        "BADUNG KAB. ID",
        "KOTA KINABALU MY",
        "LONDON AIRPORT GB",

        // Edge cases
        "SINGAPORE SG",
        "ME",  // Only country
        "",    // Empty
        "INVALID XX",  // Invalid country code
        "NO COUNTRY HERE",  // No country code

        // Real-world examples from bank statements
        "BADUNG ID",
        "BANGKOK TH",
    )

    testCases.forEach { location ->
        val result = LocationParsingService.parseLocation(location)
        val displayLocation = location.ifBlank { "(empty)" }

        println("Input:   \"$displayLocation\"")
        println("Country: ${result.countryCode ?: "(none)"}")
        println("City:    ${result.city ?: "(none)"}")
        println()
    }

    println("\n=== Location Similarity Examples ===\n")

    val similarityCases = listOf(
        Pair(
            LocationParsingService.ParsedLocation("MY", "KUALA LUMPUR"),
            LocationParsingService.ParsedLocation("MY", "KUALA LUMPUR")
        ) to "Same location",
        Pair(
            LocationParsingService.ParsedLocation("MY", "KUALA LUMPUR"),
            LocationParsingService.ParsedLocation("SG", "SINGAPORE")
        ) to "Different countries",
        Pair(
            LocationParsingService.ParsedLocation("ID", "JAKARTA"),
            LocationParsingService.ParsedLocation("ID", "BALI")
        ) to "Same country, different cities",
        Pair(
            LocationParsingService.ParsedLocation("MY", null),
            LocationParsingService.ParsedLocation("MY", null)
        ) to "Same country, both cities unknown",
    )

    similarityCases.forEach { (pair, description) ->
        val (loc1, loc2) = pair
        val similarity = LocationParsingService.locationSimilarity(loc1, loc2)
        println("$description:")
        println("  Location 1: ${loc1.city ?: "?"}, ${loc1.countryCode}")
        println("  Location 2: ${loc2.city ?: "?"}, ${loc2.countryCode}")
        println("  Similarity: $similarity")
        println()
    }

    println("\n=== City Normalization Examples ===\n")

    val normalizationCases = listOf(
        "QUILL CITY MALL",
        "LONDON AIRPORT",
        "JAKARTA STATION",
        "DUBAI CENTER",
        "SIMPLE CITY",
    )

    normalizationCases.forEach { city ->
        val normalized = LocationParsingService.normalizeCity(city)
        println("Original:   \"$city\"")
        println("Normalized: \"${normalized ?: "(none)"}\"")
        println()
    }
    }
}
