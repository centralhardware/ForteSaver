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
            println("Transaction: ${tx.date} - ${tx.type} - ${tx.amount} ${tx.currency}")
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

    @Test
    fun `test parse extracts account number`() {
        val pdfFile = File("Formed statement (2).pdf")

        if (!pdfFile.exists()) {
            println("Sample PDF file not found, skipping test")
            return
        }

        val statement = ForteBankStatementParser.parse(pdfFile)
        println("Extracted account number: '${statement.accountNumber}'")
        println("Expected: 'KZ1896502F0018918306'")

        assertNotNull(statement.accountNumber)
        assertTrue(statement.accountNumber.isNotEmpty())
        assertTrue(statement.accountNumber != "Unknown", "Account number should not be 'Unknown'")
    }

    @Test
    fun `test parse extracts account number from format 2`() {
        val pdfFile = File("Formed statement (3).pdf")

        if (!pdfFile.exists()) {
            println("Sample PDF file (format 2) not found, skipping test")
            return
        }

        val statement = ForteBankStatementParser.parse(pdfFile)
        println("Extracted account number: '${statement.accountNumber}'")
        println("Expected: 'KZ5496503F0011445795'")

        assertNotNull(statement.accountNumber)
        assertTrue(statement.accountNumber.isNotEmpty())
        assertTrue(statement.accountNumber != "Unknown", "Account number should not be 'Unknown'")
        assertTrue(statement.accountNumber == "KZ5496503F0011445795", "Expected KZ5496503F0011445795 but got ${statement.accountNumber}")
    }
}
