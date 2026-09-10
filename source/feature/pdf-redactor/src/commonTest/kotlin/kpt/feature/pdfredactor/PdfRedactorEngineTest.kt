package kpt.feature.pdfredactor

import kpt.feature.pdfredactor.engine.WatermarkRemoverEngine
import kpt.feature.pdfredactor.engine.WatermarkSignature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PdfRedactorEngineTest {

    @Test
    fun testWatermarkTargetGenerationWithEmployeeId() {
        val signature = WatermarkSignature(employeeId = "5135265")
        val targets = signature.generateMatchTargets()

        assertTrue(targets.contains("5135265"))
        assertTrue(targets.contains("EMP-5135265"))
        assertTrue(targets.contains("PNB SAKSHAM 5135265"))
    }

    @Test
    fun testWatermarkTargetGenerationWithoutEmployeeId() {
        val signature = WatermarkSignature(employeeId = null)
        val targets = signature.generateMatchTargets()

        assertTrue(targets.isEmpty())
    }

    @Test
    fun testStreamContentWatermarkRemoval() {
        val signature = WatermarkSignature(employeeId = "5135265")
        val inputContent = "BT /F1 12 Tf (PNB SAKSHAM 5135265 16-08-2026 11:30) Tj ET"

        val processed = WatermarkRemoverEngine.processStreamContent(inputContent, signature)

        assertFalse(processed.contains("5135265"))
        assertFalse(processed.contains("16-08-2026 11:30"))
    }
}
