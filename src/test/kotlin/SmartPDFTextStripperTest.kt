import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmartPDFTextStripperTest {

    /**
     * Test that soft hyphens are properly handled: "becom-\ning" -> "becoming"
     */
    @Test
    fun `should join soft hyphenated words`() {
        val pdfBytes = createTestPDF(listOf(
            "This is a becom-",
            "ing problem"
        ))
        
        val text = extractText(pdfBytes)
        
        assertTrue(text.contains("becoming"), "Expected 'becoming' but got: $text")
    }

    /**
     * Test broken words without hyphen: "MC\nC: 5719" -> "MCC: 5719"
     */
    @Test
    fun `should join broken words without hyphen when next line starts lowercase`() {
        val pdfBytes = createTestPDF(listOf(
            "MUJI-TRX KUALA LUMPUR MY, Malayan Banking Berhad, MC",
            "C: 5719"
        ))
        
        val text = extractText(pdfBytes)
        
        assertTrue(text.contains("MCC:"), "Expected 'MCC:' but got: $text")
        assertTrue(!text.contains("MC C:"), "Should not contain 'MC C:' but got: $text")
    }

    /**
     * Test normal line breaks with uppercase: should add space
     */
    @Test
    fun `should add space for normal line breaks`() {
        val pdfBytes = createTestPDF(listOf(
            "MUJI-TRX",
            "KUALA LUMPUR MY"
        ))
        
        val text = extractText(pdfBytes)
        
        assertTrue(text.contains("MUJI-TRX KUALA LUMPUR"), "Expected space between lines but got: $text")
    }

    /**
     * Test lowercase to lowercase join: "Kazakh\nstan" -> "Kazakhstan"
     */
    @Test
    fun `should join lowercase to lowercase without space`() {
        val pdfBytes = createTestPDF(listOf(
            "Freedom Bank Kazakh",
            "stan"
        ))
        
        val text = extractText(pdfBytes)
        
        assertTrue(text.contains("Kazakhstan"), "Expected 'Kazakhstan' but got: $text")
    }

    // Helper methods

    private fun createTestPDF(lines: List<String>): ByteArray {
        val document = PDDocument()
        val page = PDPage()
        document.addPage(page)

        val contentStream = PDPageContentStream(document, page)
        contentStream.beginText()
        contentStream.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
        contentStream.newLineAtOffset(50f, 750f)

        lines.forEachIndexed { index, line ->
            if (index > 0) {
                contentStream.newLineAtOffset(0f, -15f) // Move to next line
            }
            contentStream.showText(line)
        }

        contentStream.endText()
        contentStream.close()

        val outputStream = ByteArrayOutputStream()
        document.save(outputStream)
        document.close()

        return outputStream.toByteArray()
    }

    private fun extractText(pdfBytes: ByteArray): String {
        val document = org.apache.pdfbox.Loader.loadPDF(pdfBytes)
        val stripper = SmartPDFTextStripper()
        val text = stripper.getText(document)
        document.close()
        return text
    }
}
