package kpt.feature.longbookmerger

import kpt.feature.longbookmerger.generator.LandscapePdfGenerator
import kpt.feature.longbookmerger.parser.FinacleParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LongbookMergerTest {

    @Test
    fun testFinacleParser() {
        val csv = """
        BRANCH: Delhi Main Branch
        DATE: 2026-08-16
        Account,Party,Debit,Credit,Balance
        5001001,ABC Pvt Ltd,15000.0,0.0,15000.0
        5001002,XYZ Corp,0.0,30000.0,45000.0
        """.trimIndent()

        val report = FinacleParser.parseRawExport(csv)

        assertEquals("Delhi Main Branch", report.branchName)
        assertEquals(2, report.records.size)
        assertEquals(15000.0, report.totalDebit)
        assertEquals(30000.0, report.totalCredit)
    }

    @Test
    fun testLandscapePdfGeneration() {
        val csv = """
        BRANCH: Delhi Main Branch
        DATE: 2026-08-16
        Account,Party,Debit,Credit,Balance
        5001001,ABC Pvt Ltd,15000.0,0.0,15000.0
        """.trimIndent()

        val report = FinacleParser.parseRawExport(csv)
        val targetPath = "/tmp/test_longbook_landscape.pdf"

        val path = LandscapePdfGenerator.generateA4LandscapePdf(report, targetPath)
        assertEquals(targetPath, path)
    }
}
