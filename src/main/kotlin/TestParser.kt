import java.io.File

fun main() {
    val pdfFile = File("Formed statement (2).pdf")

    if (!pdfFile.exists()) {
        println("PDF file not found!")
        return
    }

    println("Parsing PDF file: ${pdfFile.name}")
    println("File size: ${pdfFile.length()} bytes")
    println()

    try {
        val statement = ForteBankStatementParser.parse(pdfFile)

        println("=== Bank Statement ===")
        println()
        println("Account Holder: ${statement.accountHolder}")
        println("Account Number: ${statement.accountNumber}")
        println("Currency: ${statement.currency}")
        println("Period: ${statement.period.from} - ${statement.period.to}")
        println()
        println("Opening Balance: ${statement.openingBalance}")
        println("Closing Balance: ${statement.closingBalance}")
        println()
        println("Transactions count: ${statement.transactions.size}")

        if (statement.transactions.isNotEmpty()) {
            println()
            println("=== First 10 Transactions ===")
            statement.transactions.take(10).forEachIndexed { index, tx ->
                val amount = if (tx.debit != null) {
                    "-${tx.debit}"
                } else {
                    "+${tx.credit}"
                }
                println("${index + 1}. ${tx.dateTime} - ${tx.description} - $amount ${statement.currency} (Balance: ${tx.balance})")
            }
        }
    } catch (e: Exception) {
        println("Failed to parse PDF: ${e.message}")
        e.printStackTrace()
    }
}
