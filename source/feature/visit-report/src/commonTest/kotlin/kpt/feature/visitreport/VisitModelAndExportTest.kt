package kpt.feature.visitreport

import kpt.core.model.visit.PhotoCategory
import kpt.core.model.visit.Visit
import kpt.core.model.visit.VisitPhoto
import kpt.core.model.visit.VisitStatus
import kpt.feature.visitreport.export.VisitDocxExporter
import kpt.feature.visitreport.export.VisitPdfExporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VisitModelAndExportTest {

    @Test
    fun testVisitDomainModelCreation() {
        val photo = VisitPhoto(
            id = "photo_1",
            visitId = "visit_101",
            originalPath = "/path/to/photo.jpg",
            latitude = 19.0760,
            longitude = 72.8777,
            accuracy = 4.5f,
            category = PhotoCategory.BUSINESS_PREMISES,
            caption = "Factory Entrance"
        )

        val visit = Visit(
            id = "visit_101",
            customerName = "Acme Enterprises",
            accountNumber = "1234567890",
            loanAccountNumber = "LN987654",
            address = "Industrial Zone, Sector 4",
            visitDate = "2026-08-16",
            visitTime = "11:30",
            purpose = "Stock & Premises Verification",
            officerName = "J. Doe",
            branch = "Main Branch",
            observations = "Factory operational. Machinery running smoothly.",
            recommendations = "Renew credit facility.",
            status = VisitStatus.COMPLETED,
            photos = listOf(photo)
        )

        assertEquals("visit_101", visit.id)
        assertEquals("Acme Enterprises", visit.customerName)
        assertEquals(1, visit.photos.size)
        assertEquals(PhotoCategory.BUSINESS_PREMISES, visit.photos[0].category)
        assertEquals(19.0760, visit.photos[0].latitude)
    }

    @Test
    fun testPdfExportGeneration() {
        val visit = Visit(
            id = "visit_pdf_1",
            customerName = "John Smith",
            accountNumber = "9988776655",
            visitDate = "2026-08-16",
            observations = "Verified residence and business.",
            photos = emptyList()
        )

        val result = VisitPdfExporter.generatePdfBytes(visit) { null }
        assertTrue(result.isNotEmpty())
        val header = result.copyOfRange(0, 8).decodeToString()
        assertEquals("%PDF-1.4", header)
        assertTrue(result.decodeToString().contains("%%EOF"))
    }

    @Test
    fun testDocxExportGeneration() {
        val visit = Visit(
            id = "visit_docx_1",
            customerName = "Alice Johnson",
            accountNumber = "1122334455",
            visitDate = "2026-08-16",
            observations = "Stock count completed.",
            photos = emptyList()
        )

        val result = VisitDocxExporter.generateDocxBytes(visit) { null }
        assertTrue(result.isNotEmpty())
        // ZIP local file header magic "PK\x03\x04"
        assertEquals(0x50, result[0].toInt() and 0xFF)
        assertEquals(0x4B, result[1].toInt() and 0xFF)
        assertEquals(0x03, result[2].toInt() and 0xFF)
        assertEquals(0x04, result[3].toInt() and 0xFF)
    }
}
