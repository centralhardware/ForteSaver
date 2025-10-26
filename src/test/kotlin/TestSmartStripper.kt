import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream

object TestSmartStripper {
    @JvmStatic
    fun main(args: Array<String>) {
    // Create test PDF with line break in "MCC"
    val document = PDDocument()
    val page = PDPage()
    document.addPage(page)

    val contentStream = PDPageContentStream(document, page)
    contentStream.beginText()
    contentStream.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
    
    // Line 1
    contentStream.newLineAtOffset(50f, 750f)
    contentStream.showText("MUJI-TRX KUALA LUMPUR MY, Malayan Banking Berhad, MC")
    
    // Line 2
    contentStream.newLineAtOffset(0f, -15f)
    contentStream.showText("C: 5719")

    contentStream.endText()
    contentStream.close()

    val outputStream = ByteArrayOutputStream()
    document.save(outputStream)
    document.close()

    val pdfBytes = outputStream.toByteArray()

    // Test with standard stripper
    println("=== STANDARD PDFTextStripper ===")
    val doc1 = Loader.loadPDF(pdfBytes)
    val stripper1 = PDFTextStripper()
    val text1 = stripper1.getText(doc1)
    doc1.close()
    println("Result: '$text1'")
    println()

    // Test with smart stripper
    println("=== SMART PDFTextStripper ===")
    val doc2 = Loader.loadPDF(pdfBytes)
    val stripper2 = SmartPDFTextStripper()
    val text2 = stripper2.getText(doc2)
    doc2.close()
    println("Result: '$text2'")
    println()
    
    // Check result
    if (text2.contains("MCC:")) {
        println("✅ SUCCESS: Found 'MCC:' in output")
    } else {
        println("❌ FAILED: Did not find 'MCC:' in output")
    }
    }
}
