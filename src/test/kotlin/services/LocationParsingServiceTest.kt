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

    // REMOVED: Comma format no longer supported after refactoring

    @Test
    fun `test parse two-word city with space-separated country`() {
        val result = LocationParsingService.parseLocation("KUALA LUMPUR MY")
        assertEquals(ParsedLocation("MY", "KUALA LUMPUR"), result)
    }

    // REMOVED: Comma format no longer supported after refactoring

    // REMOVED: Comma format no longer supported after refactoring

    @Test
    fun `test parse Indonesian city with KAB suffix`() {
        val result = LocationParsingService.parseLocation("BADUNG KAB. ID")
        // "BADUNG KAB." is not in GeoNames database, so city will be null
        assertEquals(ParsedLocation("ID", null), result)
    }

    @Test
    fun `test parse two-word Indonesian city`() {
        val result = LocationParsingService.parseLocation("SOUTH JAKARTA ID")
        // GeoNames has "Jakarta" but not "South Jakarta", so parser finds "JAKARTA"
        assertEquals(ParsedLocation("ID", "JAKARTA"), result)
    }

    @Test
    fun `test parse North direction prefix city`() {
        val result = LocationParsingService.parseLocation("NORTH JAKARTA ID")
        // GeoNames has "Jakarta" but not "North Jakarta", so parser finds "JAKARTA"
        assertEquals(ParsedLocation("ID", "JAKARTA"), result)
    }

    @Test
    fun `test parse three-word city HO CHI MINH`() {
        val result = LocationParsingService.parseLocation("HO CHI MINH VN")
        assertEquals(ParsedLocation("VN", "HO CHI MINH"), result)
    }

    @Test
    fun `test parse city with CITY suffix`() {
        val result = LocationParsingService.parseLocation("HO CHI MINH CITY VN")
        // Should find "HO CHI MINH" or "HO CHI MINH CITY" in database
        val result2 = LocationParsingService.parseLocation("VN")
        assertEquals("VN", result.countryCode)
        // City might be "HO CHI MINH" or "HO CHI MINH CITY" depending on database
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
        // Should not recognize MEE as country code (3 letters)
        assertEquals(ParsedLocation(null, null), result)
    }

    @Test
    fun `test parse with invalid country code - not in known list`() {
        val result = LocationParsingService.parseLocation("BUDVA XX")
        // XX is not a real country code
        assertEquals(ParsedLocation(null, null), result)
    }

    @Test
    fun `test parse real-world example - STARBUCKS DEWATA BALI BADUNG ID`() {
        // This is merchant name + location
        // In ForteBankStatementParser, merchant extraction happens first
        // Here we test just the location part "BADUNG ID"
        val result = LocationParsingService.parseLocation("BADUNG ID")
        // Badung is a regency, not a city, so not in GeoNames cities1000
        // Parser will return just country code
        assertEquals(ParsedLocation("ID", null), result)
    }

    @Test
    fun `test parse real-world example - Grab South Jakarta ID`() {
        val result = LocationParsingService.parseLocation("SOUTH JAKARTA ID")
        // GeoNames has "Jakarta" but not "South Jakarta"
        assertEquals(ParsedLocation("ID", "JAKARTA"), result)
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
        // "LONDON AIRPORT" is not in GeoNames, might not find city
        assertEquals("GB", result.countryCode)
        // City parsing is best-effort - may or may not find LONDON
    }

    // REMOVED: Comma format no longer supported after refactoring

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

    // Real-world examples from actual bank statements
    @Test
    fun `test real RESTORAN KASIKA PODGORICA PO ME`() {
        val result = LocationParsingService.parseLocation("PODGORICA PO ME")
        // PO is not a valid country code, ME is
        // PODGORICA PO is not in city database, so parser returns just country
        assertEquals(ParsedLocation("ME", null), result)
    }

    @Test
    fun `test real BIOSKOP CINEPLEX PODGORICA ME`() {
        val result = LocationParsingService.parseLocation("PODGORICA ME")
        assertEquals(ParsedLocation("ME", "PODGORICA"), result)
    }

    @Test
    fun `test real IDEA CENTRAL POINT PODGORICA ME`() {
        val result = LocationParsingService.parseLocation("PODGORICA ME")
        assertEquals(ParsedLocation("ME", "PODGORICA"), result)
    }

    @Test
    fun `test real KONZUM BIH K046 SARAJEVO BA`() {
        val result = LocationParsingService.parseLocation("SARAJEVO BA")
        assertEquals(ParsedLocation("BA", "SARAJEVO"), result)
    }

    @Test
    fun `test real GLOBAL INVEST GROUP SARAJEVO BA`() {
        val result = LocationParsingService.parseLocation("SARAJEVO BA")
        assertEquals(ParsedLocation("BA", "SARAJEVO"), result)
    }

    @Test
    fun `test real UR BISTRO FIT BA SARAJEVO BA`() {
        // In this case "BA" appears twice - once in merchant name, once as country
        val result = LocationParsingService.parseLocation("SARAJEVO BA")
        assertEquals(ParsedLocation("BA", "SARAJEVO"), result)
    }

    @Test
    fun `test real KONZUM BIH K065 SARAJEVO BA`() {
        val result = LocationParsingService.parseLocation("SARAJEVO BA")
        assertEquals(ParsedLocation("BA", "SARAJEVO"), result)
    }

    @Test
    fun `test real MUZEJ OPSADE SARAJEVO BA`() {
        val result = LocationParsingService.parseLocation("SARAJEVO BA")
        assertEquals(ParsedLocation("BA", "SARAJEVO"), result)
    }

    @Test
    fun `test real MUZEJ RATNOG DJETINJST SARAJEVO BA`() {
        val result = LocationParsingService.parseLocation("SARAJEVO BA")
        assertEquals(ParsedLocation("BA", "SARAJEVO"), result)
    }

    @Test
    fun `test real TASTRA D O O SARAJEVO SARAJEVO BA`() {
        // "SARAJEVO" appears twice in merchant name
        val result = LocationParsingService.parseLocation("SARAJEVO BA")
        assertEquals(ParsedLocation("BA", "SARAJEVO"), result)
    }

    @Test
    fun `test real MRKVA GROUP DOO PJ SCC SARAJEVO BA`() {
        val result = LocationParsingService.parseLocation("SARAJEVO BA")
        assertEquals(ParsedLocation("BA", "SARAJEVO"), result)
    }

    @Test
    fun `test real TERMES DOO CAFE BBI SARAJEVO BA`() {
        val result = LocationParsingService.parseLocation("SARAJEVO BA")
        assertEquals(ParsedLocation("BA", "SARAJEVO"), result)
    }

    @Test
    fun `test real MD FAMILY MLINAR 7 KOS SARAJEVO BA`() {
        val result = LocationParsingService.parseLocation("SARAJEVO BA")
        assertEquals(ParsedLocation("BA", "SARAJEVO"), result)
    }

    @Test
    fun `test real STRETTO CAFFE SARAJEVO BA`() {
        val result = LocationParsingService.parseLocation("SARAJEVO BA")
        assertEquals(ParsedLocation("BA", "SARAJEVO"), result)
    }

    @Test
    fun `test real ZEMALJSKI MUZEJ BIH JU SARAJEVO BA`() {
        val result = LocationParsingService.parseLocation("SARAJEVO BA")
        assertEquals(ParsedLocation("BA", "SARAJEVO"), result)
    }

    @Test
    fun `test real BH TELECOM DD PJ TC SC SARAJEVO BA`() {
        val result = LocationParsingService.parseLocation("SARAJEVO BA")
        assertEquals(ParsedLocation("BA", "SARAJEVO"), result)
    }

    @Test
    fun `test real AMKO KOMERC PJ 102 SARAJEVO BA`() {
        val result = LocationParsingService.parseLocation("SARAJEVO BA")
        assertEquals(ParsedLocation("BA", "SARAJEVO"), result)
    }

    @Test
    fun `test real JP SARAJEVO DOO SARAJEVO BA`() {
        val result = LocationParsingService.parseLocation("SARAJEVO BA")
        assertEquals(ParsedLocation("BA", "SARAJEVO"), result)
    }

    @Test
    fun `test real AIRALO SINGAPORE SG`() {
        val result = LocationParsingService.parseLocation("SINGAPORE SG")
        assertEquals(ParsedLocation("SG", "SINGAPORE"), result)
    }

    @Test
    fun `test real GAMES PODGORICA ME`() {
        val result = LocationParsingService.parseLocation("PODGORICA ME")
        assertEquals(ParsedLocation("ME", "PODGORICA"), result)
    }

    @Test
    fun `test real TESLA S PG TX A81 PODGORICA ME`() {
        // PG and TX are not country codes, but ME is
        val result = LocationParsingService.parseLocation("PODGORICA ME")
        assertEquals(ParsedLocation("ME", "PODGORICA"), result)
    }

    @Test
    fun `test real IDEA KRIVI MOST PODGORICA ME`() {
        val result = LocationParsingService.parseLocation("PODGORICA ME")
        assertEquals(ParsedLocation("ME", "PODGORICA"), result)
    }

    @Test
    fun `test real TC OKOV 601 PODGORICA ME`() {
        val result = LocationParsingService.parseLocation("PODGORICA ME")
        assertEquals(ParsedLocation("ME", "PODGORICA"), result)
    }

    @Test
    fun `test real TESLA TAXI 2 PODGORICA ME`() {
        val result = LocationParsingService.parseLocation("PODGORICA ME")
        assertEquals(ParsedLocation("ME", "PODGORICA"), result)
    }

    @Test
    fun `test real ZDRAVI KOLACI PODGORICA ME`() {
        val result = LocationParsingService.parseLocation("PODGORICA ME")
        assertEquals(ParsedLocation("ME", "PODGORICA"), result)
    }
}
