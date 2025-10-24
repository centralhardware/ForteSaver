import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ForteBankStatementParserTest {

    @Test
    fun `test parse sample PDF statement`() {
        val pdfFile = File("Formed statement (2).pdf")

        if (!pdfFile.exists()) {
            println("Sample PDF file not found, skipping test")
            return
        }

        val statement = ForteBankStatementParser.parse(pdfFile)

        assertNotNull(statement)
        println("Account Holder: ${statement.accountHolder}")
        println("Account Number: ${statement.accountNumber}")
        println("Currency: ${statement.currency}")
        println("Period: ${statement.period.from} - ${statement.period.to}")
        println("Opening Balance: ${statement.openingBalance}")
        println("Closing Balance: ${statement.closingBalance}")
        println("Transactions count: ${statement.transactions.size}")

        statement.transactions.take(5).forEach { tx ->
            println("Transaction: ${tx.dateTime} - ${tx.description} - ${tx.debit ?: tx.credit}")
        }
    }

    @Test
    fun `test parse extracts account holder`() {
        val pdfFile = File("Formed statement (2).pdf")

        if (!pdfFile.exists()) {
            println("Sample PDF file not found, skipping test")
            return
        }

        val statement = ForteBankStatementParser.parse(pdfFile)
        assertTrue(statement.accountHolder.isNotEmpty())
        assertTrue(statement.accountHolder != "Unknown")
    }
}
