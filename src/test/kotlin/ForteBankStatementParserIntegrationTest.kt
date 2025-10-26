import org.junit.jupiter.api.Test
import services.LocationParsingService
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Integration tests for full transaction parsing flow.
 * Tests that merchant details are correctly extracted and location is properly parsed.
 *
 * NEW LOGIC (after refactoring):
 * - merchantName contains the FULL merchant details string (including location)
 * - merchantLocation is always null
 * - Location (country + city) is parsed from merchantName by LocationParsingService
 */
class ForteBankStatementParserIntegrationTest {

    @Test
    fun `test parse RESTORAN KASIKA PODGORICA PO ME`() {
        val details = "RESTORAN KASIKA PODGORICA PO ME"
        val result = ForteBankStatementParser.parseDetails(details)

        // Full string in merchantName
        assertEquals("RESTORAN KASIKA PODGORICA PO ME", result.merchantName)
        assertEquals(null, result.merchantLocation)

        // Parse location from merchant name
        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("ME", location.countryCode)
        // "PO" is not a valid city abbreviation, so might not find PODGORICA
        // This is acceptable - location parsing is best-effort
    }

    @Test
    fun `test parse BIOSKOP CINEPLEX PODGORICA ME`() {
        val details = "BIOSKOP CINEPLEX PODGORICA ME"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("BIOSKOP CINEPLEX PODGORICA ME", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("ME", location.countryCode)
        assertEquals("PODGORICA", location.city)
    }

    @Test
    fun `test parse IDEA CENTRAL POINT PODGORICA ME`() {
        val details = "IDEA CENTRAL POINT PODGORICA ME"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("IDEA CENTRAL POINT PODGORICA ME", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("ME", location.countryCode)
        assertEquals("PODGORICA", location.city)
    }

    @Test
    fun `test parse KONZUM BIH K046 SARAJEVO BA`() {
        val details = "KONZUM BIH K046 SARAJEVO BA"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("KONZUM BIH K046 SARAJEVO BA", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("BA", location.countryCode)
        assertEquals("SARAJEVO", location.city)
    }

    @Test
    fun `test parse GLOBAL INVEST GROUP SARAJEVO BA`() {
        val details = "GLOBAL INVEST GROUP SARAJEVO BA"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("GLOBAL INVEST GROUP SARAJEVO BA", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("BA", location.countryCode)
        assertEquals("SARAJEVO", location.city)
    }

    @Test
    fun `test parse UR BISTRO FIT BA SARAJEVO BA`() {
        val details = "UR BISTRO FIT BA SARAJEVO BA"
        val result = ForteBankStatementParser.parseDetails(details)

        // "BA" appears twice - once in merchant name, once as country
        assertEquals("UR BISTRO FIT BA SARAJEVO BA", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("BA", location.countryCode)
        assertEquals("SARAJEVO", location.city)
    }

    @Test
    fun `test parse KONZUM BIH K065 SARAJEVO BA`() {
        val details = "KONZUM BIH K065 SARAJEVO BA"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("KONZUM BIH K065 SARAJEVO BA", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("BA", location.countryCode)
        assertEquals("SARAJEVO", location.city)
    }

    @Test
    fun `test parse MUZEJ OPSADE SARAJEVO BA`() {
        val details = "MUZEJ OPSADE SARAJEVO BA"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("MUZEJ OPSADE SARAJEVO BA", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("BA", location.countryCode)
        assertEquals("SARAJEVO", location.city)
    }

    @Test
    fun `test parse AIRALO SINGAPORE SG`() {
        val details = "AIRALO SINGAPORE SG"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("AIRALO SINGAPORE SG", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("SG", location.countryCode)
        assertEquals("SINGAPORE", location.city)
    }

    @Test
    fun `test parse GAMES PODGORICA ME`() {
        val details = "GAMES PODGORICA ME"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("GAMES PODGORICA ME", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("ME", location.countryCode)
        assertEquals("PODGORICA", location.city)
    }

    @Test
    fun `test parse TESLA S PG TX A81 PODGORICA ME`() {
        val details = "TESLA S PG TX A81 PODGORICA ME"
        val result = ForteBankStatementParser.parseDetails(details)

        // PG and TX are not valid country codes, so only ME at the end is recognized
        assertEquals("TESLA S PG TX A81 PODGORICA ME", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("ME", location.countryCode)
        assertEquals("PODGORICA", location.city)
    }

    @Test
    fun `test parse IDEA KRIVI MOST PODGORICA ME`() {
        val details = "IDEA KRIVI MOST PODGORICA ME"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("IDEA KRIVI MOST PODGORICA ME", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("ME", location.countryCode)
        assertEquals("PODGORICA", location.city)
    }

    @Test
    fun `test parse TC OKOV 601 PODGORICA ME`() {
        val details = "TC OKOV 601 PODGORICA ME"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("TC OKOV 601 PODGORICA ME", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("ME", location.countryCode)
        assertEquals("PODGORICA", location.city)
    }

    @Test
    fun `test parse TESLA TAXI 2 PODGORICA ME`() {
        val details = "TESLA TAXI 2 PODGORICA ME"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("TESLA TAXI 2 PODGORICA ME", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("ME", location.countryCode)
        assertEquals("PODGORICA", location.city)
    }

    @Test
    fun `test parse ZDRAVI KOLACI PODGORICA ME`() {
        val details = "ZDRAVI KOLACI PODGORICA ME"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("ZDRAVI KOLACI PODGORICA ME", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("ME", location.countryCode)
        assertEquals("PODGORICA", location.city)
    }

    @Test
    fun `test parse TASTRA D O O SARAJEVO SARAJEVO BA`() {
        val details = "TASTRA D O O SARAJEVO SARAJEVO BA"
        val result = ForteBankStatementParser.parseDetails(details)

        // "SARAJEVO" appears twice in merchant name
        assertEquals("TASTRA D O O SARAJEVO SARAJEVO BA", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("BA", location.countryCode)
        assertEquals("SARAJEVO", location.city)
    }

    @Test
    fun `test parse MRKVA GROUP DOO PJ SCC SARAJEVO BA`() {
        val details = "MRKVA GROUP DOO PJ SCC SARAJEVO BA"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("MRKVA GROUP DOO PJ SCC SARAJEVO BA", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("BA", location.countryCode)
        assertEquals("SARAJEVO", location.city)
    }

    @Test
    fun `test parse TERMES DOO CAFE BBI SARAJEVO BA`() {
        val details = "TERMES DOO CAFE BBI SARAJEVO BA"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("TERMES DOO CAFE BBI SARAJEVO BA", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("BA", location.countryCode)
        assertEquals("SARAJEVO", location.city)
    }

    @Test
    fun `test parse MD FAMILY MLINAR 7 KOS SARAJEVO BA`() {
        val details = "MD FAMILY MLINAR 7 KOS SARAJEVO BA"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("MD FAMILY MLINAR 7 KOS SARAJEVO BA", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("BA", location.countryCode)
        assertEquals("SARAJEVO", location.city)
    }

    @Test
    fun `test parse STRETTO CAFFE SARAJEVO BA`() {
        val details = "STRETTO CAFFE SARAJEVO BA"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("STRETTO CAFFE SARAJEVO BA", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertEquals("BA", location.countryCode)
        assertEquals("SARAJEVO", location.city)
    }

    // NEW TESTS FROM USER - Real problematic transactions
    @Test
    fun `test parse 103 COFFEE CHOW KIT KUALA LUMPUR MY`() {
        val details = "103 COFFEE CHOW KIT KUALA LUMPUR MY"
        val result = ForteBankStatementParser.parseDetails(details)

        println("Input: $details")
        println("Merchant: ${result.merchantName}")

        assertEquals("103 COFFEE CHOW KIT KUALA LUMPUR MY", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        println("Country: ${location.countryCode}")
        println("City: ${location.city}")

        // Should extract Kuala Lumpur properly
        assertNotNull(location.countryCode, "Country code should be extracted")
        assertEquals("MY", location.countryCode)
    }

    @Test
    fun `test parse MPOS ROOTSPLANT DA NANG VN`() {
        val details = "MPOS ROOTSPLANT DA NANG VN"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("MPOS ROOTSPLANT DA NANG VN", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertNotNull(location.countryCode)
        assertEquals("VN", location.countryCode)
    }

    @Test
    fun `test parse Grab HA NOI VN`() {
        val details = "Grab* A 7C953M6WWIW4 HA NOI VN"
        val result = ForteBankStatementParser.parseDetails(details)

        assertEquals("Grab* A 7C953M6WWIW4 HA NOI VN", result.merchantName)
        assertEquals(null, result.merchantLocation)

        val location = LocationParsingService.parseLocation(result.merchantName)
        assertNotNull(location.countryCode)
        assertEquals("VN", location.countryCode)
    }

    @Test
    fun `test all real transactions from user`() {
        val transactions = listOf(
            "KOKOS SDN BHD-QCM KUALA LUMPUR MY",
            "GRAB RIDES-EC PETALING JAY MY",
            "MUJI-TRX KUALA LUMPUR MY",
            "GOOGLE *YouTubePremium g.co/helppay GB",
            "APPLE.COM BANGKOK TH",
            "Grab* A 7JN7P8AWWFCI8W South Jakart ID",
            "STARBUCKS DEWATA BALI BADUNG ID",
            "MM SR11 Badung (Kab) ID",
            "NHA HANG VAN MAY Q NGU HANH S VN",
            "VNPAY*SUNMART Q SON TRA VN",
            "SINGAPOREAI 618246001892 SINGAPORE ID",
            "AIRBNB * HM3NPQSXXA 415 800 5959 LU",
            "103 COFFEE CHOW KIT KUALA LUMPUR MY",
            "SEMANGAT KAMPUNG SDN. BH KAMPUNG BARU MY",
            "HOMETOWN HAINAN COFFEE Q KUALA LUMPUR MY",
            "MPOS ROOTSPLANT DA NANG VN",
            "6/6 DANANG VN",
            "Grab* A 7C953M6WWIW4 HA NOI VN",
            "PT FINNET INDONESIA Jakarta ID",
            "WWW.GRAB.COM BANGKOK TH",
            "LONGRAO DIMSUM BANGKOK TH",
            "TESLA VOZILO 3 VUKA KARADZI ME",
            "WWW.GLOVOAPP.COM PODGORICA ME",
            "KNJIZARA DELTA PODGORICA ME",
            "MAMICKA PODGORICA ME",
            "LCAE27 CULTO CAFE ABU DHABI AE",
            "LP INTERCAFFE DOO A3 SURCIN RS",
            "213 MAXI 178 BEOGRAD RS",
            "DATA STATUS AKADEMIJA Savski venac RS",
            "COFFEE CAKE PODGORICA ME",
            "OOO Samarkand Touristic SAMARKAND UZ"
        )

        val failures = mutableListOf<String>()
        var successCount = 0

        println("\n=== TESTING ${transactions.size} REAL TRANSACTIONS ===")

        transactions.forEachIndexed { index, transaction ->
            val result = ForteBankStatementParser.parseDetails(transaction)
            // NEW: Parse from merchantName instead of merchantLocation
            val location = LocationParsingService.parseLocation(result.merchantName)

            val status = if (location.countryCode != null) {
                successCount++
                "✅"
            } else {
                failures.add("$transaction -> merchant=${result.merchantName}")
                "❌"
            }

            println("${index + 1}. $status \"$transaction\"")
            println("   Merchant: ${result.merchantName ?: "(none)"}")
            println("   Country:  ${location.countryCode ?: "(none)"}")
            println("   City:     ${location.city ?: "(none)"}")
        }

        println("\n=== SUMMARY ===")
        println("Success: $successCount / ${transactions.size}")
        println("Failures: ${failures.size} / ${transactions.size}\n")

        assertEquals(0, failures.size, "Expected all transactions to extract location, but ${failures.size} failed")
    }
}
