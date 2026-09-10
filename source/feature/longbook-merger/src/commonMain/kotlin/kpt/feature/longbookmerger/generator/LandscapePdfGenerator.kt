package kpt.feature.longbookmerger.generator

import kpt.feature.longbookmerger.parser.LongbookReport

object LandscapePdfGenerator {

    fun generateA4LandscapePdf(report: LongbookReport, outputPath: String): String {
        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        sb.append("% CBSL / FINACLE LONGBOOK LANDSCAPE A4 PDF\n")
        sb.append("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n")
        sb.append("2 0 obj << /Type /Pages /Kinds [/Page] /Count 1 /Kids [3 0 R] >> endobj\n")
        // MediaBox [0 0 842 595] defines A4 Landscape orientation
        sb.append("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] /Contents 4 0 R >> endobj\n")

        val contentText = buildString {
            append("BT /F1 16 Tf 40 550 Td (${report.title.replace("(", "[").replace(")", "]")}) Tj ET\n")
            append("BT /F1 11 Tf 40 525 Td (Branch: ${report.branchName}  |  Report Date: ${report.reportDate}) Tj ET\n")
            append("BT /F1 11 Tf 40 500 Td (Total Debit: ${report.totalDebit}  |  Total Credit: ${report.totalCredit}) Tj ET\n")
            append("BT /F1 10 Tf 40 470 Td (Account No      Party Name              Debit (INR)    Credit (INR)   Balance (INR)) Tj ET\n")

            var y = 445
            report.records.take(20).forEach { rec ->
                if (y > 60) {
                    append("BT /F1 9 Tf 40 $y Td (${rec.accountNumber.padEnd(16)} ${rec.partyName.take(20).padEnd(23)} ${rec.debitAmount.toString().padEnd(14)} ${rec.creditAmount.toString().padEnd(14)} ${rec.balance}) Tj ET\n")
                    y -= 18
                }
            }
        }

        sb.append("4 0 obj << /Length ${contentText.length} >> stream\n")
        sb.append(contentText)
        sb.append("\nendstream endobj\n")
        sb.append("xref\n0 5\n0000000000 65535 f \n")
        sb.append("trailer << /Size 5 /Root 1 0 R >>\nstartxref\n500\n%%EOF")

        return outputPath
    }
}
