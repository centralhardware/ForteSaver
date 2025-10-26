import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.io.IOException

/**
 * PDFTextStripper that controls spacing based on actual character positions in PDF.
 * 
 * Problem: PDFBox inserts spaces/newlines where it thinks they should be, but this breaks words:
 * - "MC" + newline + "C: 5719" -> "MC\nC: 5719" -> after processing -> "MC C: 5719" (WRONG!)
 * 
 * Solution: Use X/Y coordinates to determine if we need space or not.
 * - If characters are on same Y (same line), use X distance to decide on space
 * - If characters are on different Y (different lines), DON'T automatically add space
 */
class SmartPDFTextStripper : PDFTextStripper() {
    
    private val allPositions = mutableListOf<TextPosition>()
    
    @Throws(IOException::class)
    override fun writeString(text: String, textPositions: List<TextPosition>) {
        // Don't write anything yet - just collect positions
        allPositions.addAll(textPositions)
    }
    
    @Throws(IOException::class)
    override fun endDocument(document: org.apache.pdfbox.pdmodel.PDDocument) {
        // Now process all positions and write with correct spacing
        val result = buildTextFromPositions(allPositions)
        output.write(result)
        
        // Clear for next document
        allPositions.clear()
        
        super.endDocument(document)
    }
    
    private fun buildTextFromPositions(positions: List<TextPosition>): String {
        if (positions.isEmpty()) return ""
        
        val result = StringBuilder()
        var lastPos: TextPosition? = null
        
        for (pos in positions) {
            if (lastPos == null) {
                result.append(pos.unicode)
                lastPos = pos
                continue
            }
            
            val lastY = lastPos.yDirAdj
            val currentY = pos.yDirAdj
            val lastX = lastPos.xDirAdj + lastPos.width
            val currentX = pos.xDirAdj
            
            // Check if on same line (Y coordinate similar)
            val yDiff = Math.abs(currentY - lastY)
            val onSameLine = yDiff < 2.0f
            
            if (onSameLine) {
                // Same line - check X distance
                val gap = currentX - lastX
                val avgCharWidth = lastPos.width
                
                // If gap is significant (more than half char width), add space
                if (gap > avgCharWidth * 0.5f) {
                    result.append(' ')
                }
            } else {
                // Different line - check if this looks like a word continuation
                // Strategy: if previous line is very short and ends mid-word, might be continuation
                // For now: add newline, let parser handle it
                result.append('\n')
            }
            
            result.append(pos.unicode)
            lastPos = pos
        }
        
        return result.toString()
    }
}

