fun main() {
    val tx = "103 COFFEE CHOW KIT KUALA LUMPUR MY"
    val result = ForteBankStatementParser.parseDetails(tx)
    
    println("Input: $tx")
    println("Merchant: ${result.merchantName}")
    println("Location: ${result.merchantLocation}")
    
    val loc = services.LocationParsingService.parseLocation(result.merchantLocation ?: "")
    println("Country: ${loc.countryCode}")
    println("City: ${loc.city}")
}
