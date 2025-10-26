import org.junit.jupiter.api.Test
import services.LocationParsingService

class RealTransactionsTest {

    @Test
    fun `test real transactions location parsing`() {
        val transactions = listOf(
            "AIRBNB * HM3NPQSXXA 415 800 5959 LU",
            "103 COFFEE CHOW KIT KUALA LUMPUR MY",
            "SEMANGAT KAMPUNG SDN. BH KAMPUNG BARU MY",
            "HOMETOWN HAINAN COFFEE Q KUALA LUMPUR MY",
            "AIRBNB * INC 415 800 5959 LU",
            "AIRBNB * HMR85YDYFR 415 800 5959 LU",
            "MPOS ROOTSPLANT DA NANG VN",
            "6/6 DANANG VN",
            "Grab* A 7C953M6WWIW4 HA NOI VN",
            "PT FINNET INDONESIA Jakarta ID",
            "SINGAPOREAI 618246001892 SINGAPORE ID",
            "AIRBNB * HMRBYQFKHC 415 800 5959 LU",
            "Grab* A 7C6CEIPGWG24 HA NOI VN",
            "VNPAY OP LA EXPRESS 2 DA NANG VN",
            "MPOS HIVE DA NANG VN",
            "Grab* A 7C53GBOWWI6G HA NOI VN",
            "Grab* A 7C522Q7GWFGO HA NOI VN",
            "Grab* A 7CXOUDNWWF6C HA NOI VN",
            "TROPICAL COFFEE DA NANG VN",
            "Grab* A 7CWT6JCWWEB2 HA NOI VN",
            "HKD BE MAN 2 DA NANG VN",
            "Grab* A 7BU5IJLGWFM3 HA NOI VN",
            "NHA BEP XUA TP. DA NANG VN",
            "Grab* A 7BTADANWWHM8 HA NOI VN",
            "Grab* A 7BT8RFUGWFEF HA NOI VN",
            "MPOS WAIKIKI DA NANG VN",
            "Grab* A 7BSSWJIGWFPM HA NOI VN",
            "Grab* A 7BSOOUAGWILP HA NOI VN",
            "Grab* A 7BSHT3PWWGPG HA NOI VN",
            "VNPAY*VTSHNI18 HA NOI VN",
            "Grab* A 7BPGDUDWWHF6 HA NOI VN",
            "WWW.GRAB.COM BANGKOK TH",
            "LONGRAO DIMSUM BANGKOK TH",
            "JONES SALAD(SALADAENG) BANGKOK TH",
            "461 SCT ICONSIAM 7TH BANGKOK TH",
            "SUPER TURTLE PUBLIC LIMI BANGKOK TH",
            "J.I.B. SIAM PARAGON BANGKOK TH",
            "SINGAPOREAI 2459851777 VIETNAM VN",
            "7 11 TEMPO GRAND BANGKHO CHOM TH",
            "459 SCT NST ONE SILOM BANGKOK TH",
            "TICKETMELON EDC2 BANGKOK TH",
            "FFPAY*TICKETS ALMATY KZ",
            "TESLA VOZILO 3 VUKA KARADZI ME",
            "TESLA VOZILO 12 8 MARTA BB P ME",
            "WWW.GLOVOAPP.COM PODGORICA ME",
            "TESLA VOZILO 18 8 MARTA BB P ME",
            "KNJIZARA DELTA PODGORICA ME",
            "MAMICKA PODGORICA ME",
            "LCAE27 CULTO CAFE ABU DHABI AE",
            "LP INTERCAFFE DOO A3 SURCIN RS",
            "213 MAXI 178 BEOGRAD RS",
            "DATA STATUS AKADEMIJA Savski venac RS",
            "COFFEE CAKE PODGORICA ME",
            "OOO Samarkand Touristic SAMARKAND UZ",
            "Z SHOP BIG FASHI PODGORICA ME"
        )

        println("\n=== REAL TRANSACTIONS PARSING RESULTS ===\n")

        transactions.forEachIndexed { index, transaction ->
            val result = ForteBankStatementParser.parseDetails(transaction)
            val location = LocationParsingService.parseLocation(result.merchantLocation)

            println("${index + 1}. \"$transaction\"")
            println("   Merchant: ${result.merchantName ?: "(none)"}")
            println("   Location: ${result.merchantLocation ?: "(none)"}")
            println("   Country:  ${location.countryCode ?: "(none)"}")
            println("   City:     ${location.city ?: "(none)"}")
            println()
        }
    }
}
