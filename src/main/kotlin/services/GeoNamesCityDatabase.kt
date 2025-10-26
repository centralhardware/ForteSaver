package services

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * Offline city database using GeoNames cities1000.txt file.
 * Loads ~130,000 cities from resources on startup.
 *
 * File format (tab-separated):
 * geonameid, name, asciiname, alternatenames, lat, lon, feature_class, feature_code,
 * country_code, cc2, admin1, admin2, admin3, admin4, population, elevation, dem,
 * timezone, modification_date
 */
object GeoNamesCityDatabase {
    private val logger = LoggerFactory.getLogger(GeoNamesCityDatabase::class.java)

    data class City(
        val name: String,
        val asciiName: String,
        val countryCode: String,
        val alternateNames: Set<String> = emptySet()
    )

    // Map: Country Code -> Set of City Names (all variants, normalized to uppercase)
    private val citiesByCountry = ConcurrentHashMap<String, MutableSet<String>>()

    // Total cities loaded
    @Volatile
    private var totalCitiesLoaded = 0

    init {
        loadCities()
    }

    /**
     * Load cities from cities1000.txt resource file.
     */
    private fun loadCities() {
        val startTime = System.currentTimeMillis()
        logger.info("Loading cities from GeoNames cities1000.txt...")

        try {
            val inputStream = javaClass.classLoader.getResourceAsStream("cities1000.txt")
            if (inputStream == null) {
                logger.warn("cities1000.txt not found in resources, using fallback empty database")
                return
            }

            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                reader.lineSequence().forEach { line ->
                    try {
                        parseCityLine(line)
                    } catch (e: Exception) {
                        logger.debug("Failed to parse line: ${e.message}")
                    }
                }
            }

            val duration = System.currentTimeMillis() - startTime
            logger.info("Loaded $totalCitiesLoaded cities from ${citiesByCountry.size} countries in ${duration}ms")
        } catch (e: Exception) {
            logger.error("Failed to load cities database", e)
        }
    }

    /**
     * Parse a single line from cities1000.txt.
     */
    private fun parseCityLine(line: String) {
        val parts = line.split('\t')
        if (parts.size < 9) return

        val name = parts[1].trim()
        val asciiName = parts[2].trim()
        val alternateNamesRaw = parts[3].trim()
        val countryCode = parts[8].trim().uppercase()

        if (name.isBlank() || countryCode.isBlank()) return

        // Get or create set for this country
        val citySet = citiesByCountry.getOrPut(countryCode) { mutableSetOf() }

        // Add main name
        citySet.add(name.uppercase())

        // Add ASCII name if different
        if (asciiName.isNotBlank() && asciiName != name) {
            citySet.add(asciiName.uppercase())
        }

        // Add alternate names
        if (alternateNamesRaw.isNotBlank()) {
            alternateNamesRaw.split(',').forEach { altName ->
                val cleaned = altName.trim()
                if (cleaned.isNotBlank() && cleaned.length < 50) { // Skip очень длинные имена
                    citySet.add(cleaned.uppercase())
                }
            }
        }

        totalCitiesLoaded++
    }

    /**
     * Check if a city exists in the given country.
     *
     * @param cityName City name to check (case-insensitive)
     * @param countryCode ISO 3166-1 alpha-2 country code
     * @return true if the city is in our database, false otherwise
     */
    fun isCityInCountry(cityName: String, countryCode: String): Boolean {
        if (cityName.isBlank() || countryCode.isBlank()) {
            return false
        }

        val normalizedCity = cityName.trim().uppercase()
        val normalizedCountry = countryCode.trim().uppercase()

        val countryCities = citiesByCountry[normalizedCountry]
        if (countryCities == null) {
            logger.debug("Country $normalizedCountry not in database")
            // Unknown country - assume city exists (fail open)
            return true
        }

        val exists = countryCities.contains(normalizedCity)
        logger.debug("City check: $normalizedCity in $normalizedCountry = $exists")

        return exists
    }

    /**
     * Get statistics about the database.
     */
    fun getStats(): String {
        val totalCityNames = citiesByCountry.values.sumOf { it.size }
        val totalCountries = citiesByCountry.size
        return "GeoNames database: $totalCountries countries, $totalCitiesLoaded cities, $totalCityNames total city names (including alternates)"
    }

    /**
     * Check if database is loaded.
     */
    fun isLoaded(): Boolean = totalCitiesLoaded > 0
}
