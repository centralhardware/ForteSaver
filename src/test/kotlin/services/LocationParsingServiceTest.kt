package services

import org.junit.jupiter.api.Test
import services.LocationParsingService.ParsedLocation
import kotlin.test.assertEquals

class LocationParsingServiceTest {

    @Test
    fun `test parse simple single-word city with space-separated country`() {
        val result = LocationParsingService.parseLocation("BUDVA ME")
        assertEquals(ParsedLocation("ME", "BUDVA"), result)
    }

    @Test
    fun `test parse single-word city with comma-separated country`() {
        val result = LocationParsingService.parseLocation("BUDVA,ME")
        assertEquals(ParsedLocation("ME", "BUDVA"), result)
    }

    @Test
    fun `test parse two-word city with space-separated country`() {
        val result = LocationParsingService.parseLocation("KUALA LUMPUR MY")
        assertEquals(ParsedLocation("MY", "KUALA LUMPUR"), result)
    }

    @Test
    fun `test parse two-word city with comma-separated country`() {
        val result = LocationParsingService.parseLocation("KUALA LUMPUR,MY")
        assertEquals(ParsedLocation("MY", "KUALA LUMPUR"), result)
    }

    @Test
    fun `test parse city with merchant prefix and comma`() {
        val result = LocationParsingService.parseLocation("QUILL CITY MALL,KUALA LUMPUR,MY")
        assertEquals(ParsedLocation("MY", "KUALA LUMPUR"), result)
    }

    @Test
    fun `test parse Indonesian city with KAB suffix`() {
        val result = LocationParsingService.parseLocation("BADUNG KAB. ID")
        assertEquals(ParsedLocation("ID", "BADUNG KAB."), result)
    }

    @Test
    fun `test parse two-word Indonesian city`() {
        val result = LocationParsingService.parseLocation("SOUTH JAKARTA ID")
        assertEquals(ParsedLocation("ID", "SOUTH JAKARTA"), result)
    }

    @Test
    fun `test parse North direction prefix city`() {
        val result = LocationParsingService.parseLocation("NORTH JAKARTA ID")
        assertEquals(ParsedLocation("ID", "NORTH JAKARTA"), result)
    }

    @Test
    fun `test parse three-word city HO CHI MINH`() {
        val result = LocationParsingService.parseLocation("HO CHI MINH VN")
        assertEquals(ParsedLocation("VN", "HO CHI MINH"), result)
    }

    @Test
    fun `test parse city with CITY suffix`() {
        val result = LocationParsingService.parseLocation("HO CHI MINH CITY VN")
        assertEquals(ParsedLocation("VN", "HO CHI MINH CITY"), result)
    }

    @Test
    fun `test parse London GB`() {
        val result = LocationParsingService.parseLocation("LONDON GB")
        assertEquals(ParsedLocation("GB", "LONDON"), result)
    }

    @Test
    fun `test parse Podgorica ME`() {
        val result = LocationParsingService.parseLocation("PODGORICA ME")
        assertEquals(ParsedLocation("ME", "PODGORICA"), result)
    }

    @Test
    fun `test parse with extra spaces`() {
        val result = LocationParsingService.parseLocation("  BUDVA   ME  ")
        assertEquals(ParsedLocation("ME", "BUDVA"), result)
    }

    @Test
    fun `test parse with lowercase input`() {
        val result = LocationParsingService.parseLocation("budva me")
        assertEquals(ParsedLocation("ME", "BUDVA"), result)
    }

    @Test
    fun `test parse with mixed case input`() {
        val result = LocationParsingService.parseLocation("Kuala Lumpur MY")
        assertEquals(ParsedLocation("MY", "KUALA LUMPUR"), result)
    }

    @Test
    fun `test parse null location`() {
        val result = LocationParsingService.parseLocation(null)
        assertEquals(ParsedLocation(null, null), result)
    }

    @Test
    fun `test parse empty location`() {
        val result = LocationParsingService.parseLocation("")
        assertEquals(ParsedLocation(null, null), result)
    }

    @Test
    fun `test parse blank location`() {
        val result = LocationParsingService.parseLocation("   ")
        assertEquals(ParsedLocation(null, null), result)
    }

    @Test
    fun `test parse with invalid country code - too long`() {
        val result = LocationParsingService.parseLocation("BUDVA MEE")
        // Should not recognize MEE as country code
        assertEquals(ParsedLocation(null, "BUDVA MEE"), result)
    }

    @Test
    fun `test parse with invalid country code - not in known list`() {
        val result = LocationParsingService.parseLocation("BUDVA XX")
        // XX is not a real country code
        assertEquals(ParsedLocation(null, "BUDVA XX"), result)
    }

    @Test
    fun `test parse real-world example - STARBUCKS DEWATA BALI BADUNG ID`() {
        // This is merchant name + location
        // In ForteBankStatementParser, merchant extraction happens first
        // Here we test just the location part "BADUNG ID"
        val result = LocationParsingService.parseLocation("BADUNG ID")
        assertEquals(ParsedLocation("ID", "BADUNG"), result)
    }

    @Test
    fun `test parse real-world example - Grab South Jakarta ID`() {
        val result = LocationParsingService.parseLocation("SOUTH JAKARTA ID")
        assertEquals(ParsedLocation("ID", "SOUTH JAKARTA"), result)
    }

    @Test
    fun `test parse Singapore`() {
        val result = LocationParsingService.parseLocation("SINGAPORE SG")
        assertEquals(ParsedLocation("SG", "SINGAPORE"), result)
    }

    @Test
    fun `test parse Bangkok Thailand`() {
        val result = LocationParsingService.parseLocation("BANGKOK TH")
        assertEquals(ParsedLocation("TH", "BANGKOK"), result)
    }

    @Test
    fun `test parse New York USA`() {
        val result = LocationParsingService.parseLocation("NEW YORK US")
        assertEquals(ParsedLocation("US", "NEW YORK"), result)
    }

    @Test
    fun `test parse San Francisco USA`() {
        val result = LocationParsingService.parseLocation("SAN FRANCISCO US")
        assertEquals(ParsedLocation("US", "SAN FRANCISCO"), result)
    }

    @Test
    fun `test parse Los Angeles USA`() {
        val result = LocationParsingService.parseLocation("LOS ANGELES US")
        assertEquals(ParsedLocation("US", "LOS ANGELES"), result)
    }

    @Test
    fun `test parse only country code`() {
        val result = LocationParsingService.parseLocation("ME")
        assertEquals(ParsedLocation("ME", null), result)
    }

    @Test
    fun `test parse with KOTA indicator`() {
        val result = LocationParsingService.parseLocation("KOTA KINABALU MY")
        assertEquals(ParsedLocation("MY", "KOTA KINABALU"), result)
    }

    @Test
    fun `test parse with AIRPORT indicator`() {
        val result = LocationParsingService.parseLocation("LONDON AIRPORT GB")
        assertEquals(ParsedLocation("GB", "LONDON AIRPORT"), result)
    }

    @Test
    fun `test comma format with multiple parts`() {
        val result = LocationParsingService.parseLocation("STORE,STREET,CITY,MY")
        assertEquals(ParsedLocation("MY", "CITY"), result)
    }

    @Test
    fun `test comma format with only country`() {
        val result = LocationParsingService.parseLocation("MY")
        assertEquals(ParsedLocation("MY", null), result)
    }

    @Test
    fun `test normalize city removes MALL suffix`() {
        val normalized = LocationParsingService.normalizeCity("QUILL CITY MALL")
        assertEquals("QUILL CITY", normalized)
    }

    @Test
    fun `test normalize city removes AIRPORT suffix`() {
        val normalized = LocationParsingService.normalizeCity("LONDON AIRPORT")
        assertEquals("LONDON", normalized)
    }

    @Test
    fun `test normalize city handles null`() {
        val normalized = LocationParsingService.normalizeCity(null)
        assertEquals(null, normalized)
    }

    @Test
    fun `test normalize city handles blank`() {
        val normalized = LocationParsingService.normalizeCity("   ")
        assertEquals(null, normalized)
    }

    @Test
    fun `test isHomeCountry Montenegro`() {
        val result = LocationParsingService.isHomeCountry("ME")
        assertEquals(true, result)
    }

    @Test
    fun `test isHomeCountry Serbia`() {
        val result = LocationParsingService.isHomeCountry("RS")
        assertEquals(true, result)
    }

    @Test
    fun `test isHomeCountry foreign`() {
        val result = LocationParsingService.isHomeCountry("MY")
        assertEquals(false, result)
    }

    @Test
    fun `test isHomeCountry null defaults to true`() {
        val result = LocationParsingService.isHomeCountry(null)
        assertEquals(true, result)
    }

    @Test
    fun `test locationSimilarity same location`() {
        val loc1 = ParsedLocation("MY", "KUALA LUMPUR")
        val loc2 = ParsedLocation("MY", "KUALA LUMPUR")
        val similarity = LocationParsingService.locationSimilarity(loc1, loc2)
        assertEquals(1.0, similarity)
    }

    @Test
    fun `test locationSimilarity different countries`() {
        val loc1 = ParsedLocation("MY", "KUALA LUMPUR")
        val loc2 = ParsedLocation("SG", "SINGAPORE")
        val similarity = LocationParsingService.locationSimilarity(loc1, loc2)
        assertEquals(0.0, similarity)
    }

    @Test
    fun `test locationSimilarity same country different cities`() {
        val loc1 = ParsedLocation("ID", "JAKARTA")
        val loc2 = ParsedLocation("ID", "BALI")
        val similarity = LocationParsingService.locationSimilarity(loc1, loc2)
        assertEquals(0.0, similarity)
    }

    @Test
    fun `test locationSimilarity same country both cities null`() {
        val loc1 = ParsedLocation("MY", null)
        val loc2 = ParsedLocation("MY", null)
        val similarity = LocationParsingService.locationSimilarity(loc1, loc2)
        assertEquals(0.8, similarity)
    }
}
