import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

fun main() {
    val pdfFile = File("Formed statement (2).pdf")

    if (!pdfFile.exists()) {
        println("PDF file not found!")
        return
    }

    println("Extracting text from PDF file: ${pdfFile.name}")
    println("File size: ${pdfFile.length()} bytes")
    println()

    try {
        val document: PDDocument = Loader.loadPDF(pdfFile)
        println("Number of pages: ${document.numberOfPages}")
        println()

        val stripper = PDFTextStripper()
        val text = stripper.getText(document)
        document.close()

        println("=== EXTRACTED TEXT ===")
        println(text)
        println("=== END ===")

    } catch (e: Exception) {
        println("Failed to extract text from PDF: ${e.message}")
        e.printStackTrace()
    }
}
