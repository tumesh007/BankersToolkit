package kpt.feature.visitreport.export

import kpt.core.model.visit.Visit
import kpt.core.model.visit.VisitPhoto
import kotlinx.datetime.toLocalDateTime

object VisitDocxExporter {

    /**
     * Generates an OpenXML .docx report for [visit], embedding the actual
     * captured photographs (JPEG media parts) when [photoBytes] resolves them.
     *
     * @param photoBytes resolves a [VisitPhoto] path/URI into raw JPEG bytes.
     * When `null`, a text placeholder is emitted instead of the image.
     */
    fun generateDocxBytes(visit: Visit, photoBytes: (VisitPhoto) -> ByteArray?): ByteArray {
        val images = mutableListOf<ImageData>()
        var imageCounter = 0

        fun buildImageData(photo: VisitPhoto): ImageData? {
            val raw = photoBytes(photo)?.takeIf { it.isNotEmpty() } ?: return null
            val dims = jpegDimensions(raw) ?: return null
            val (w, h) = dims
            if (w !in 2..10000 || h !in 2..10000) return null

            imageCounter++
            val transform = imgToEmu(w, h)
            return ImageData(
                relId = "rId${imageCounter + 1}", // rId1 is the document relationship
                fileName = "image$imageCounter.jpg",
                id = imageCounter,
                emuWidthPx = transform.first,
                emuHeightPx = transform.second,
                bytes = raw,
            ).also { images.add(it) }
        }

        val documentXml = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
            append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"")
            append(" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"")
            append(" xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\"")
            append(" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\"")
            append(" xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\"")
            append(" xmlns:v=\"urn:schemas-microsoft-com:vml\"")
            append(">\n")
            append("<w:body>\n")

            

            if (visit.reportFormat == kpt.core.model.visit.ReportFormat.GENERAL) {
                heading("1. VISIT / CUSTOMER DETAILS")
                field("Customer Name", visit.customerName)
                field("CIF / Customer ID", visit.referenceNumber ?: "N/A")
                field("Loan Account No.", visit.loanAccountNumber)
                field("Address", visit.address)
                field("Visit Date", visit.visitDate)
                field("Visit Time", visit.visitTime)
                field("Officer", "${visit.officerName} (${visit.branch})")

                line()
                heading("2. PURPOSE / VISIT DETAILS")
                field("Purpose of Visit", visit.purpose)
                field("Visit Type", visit.status.name)

                line()
                heading("3. OFFICER OBSERVATIONS")
                para(visit.observations)

                line()
                heading("4. RECOMMENDATIONS")
                para(visit.recommendations)

                line()
                heading("5. REMARKS / CONCLUSION")
                para(visit.remarks)

                line()
            }

            if (visit.reportFormat != kpt.core.model.visit.ReportFormat.GENERAL) {
                ProformaTemplates.injectProformaFields(
                    visit = visit,
                    drawHeading = { heading(it) },
                    drawField = { key, value -> field(key, value) }
                )
                line()
            }

            if (visit.photos.isNotEmpty()) {
                pageBreak()
                heading("6. FIELD PHOTOGRAPHS (${visit.photos.size})")
                visit.photos.forEachIndexed { idx, photo ->
                    val image = buildImageData(photo)
                    append("<w:p><w:pPr><w:keepNext/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"24\"/></w:rPr><w:t xml:space=\"preserve\">Photograph #${idx + 1}</w:t></w:r></w:p>\n")

                    append("<w:tbl>")
                    append("<w:tblPr><w:tblBorders><w:top w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/><w:left w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/><w:bottom w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/><w:right w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/></w:tblBorders><w:tblW w:w=\"5000\" w:type=\"pct\"/></w:tblPr>")

                    append("<w:tr>")

                    // Col 1: Photo (65% width)
                    append("<w:tc><w:tcPr><w:tcW w:w=\"3250\" w:type=\"pct\"/><w:vAlign w:val=\"center\"/></w:tcPr>")
                    if (image != null) {
                        append(
                            imageDrawing(image.relId, image.id, image.emuWidthPx * EMU_PER_PX, image.emuHeightPx * EMU_PER_PX),
                        )
                    } else {
                        append("<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r><w:t>Photo Not Available</w:t></w:r></w:p>")
                    }
                    append("</w:tc>")

                    // Col 2: Meta details (35% width)
                    append("<w:tc><w:tcPr><w:tcW w:w=\"1750\" w:type=\"pct\"/><w:vAlign w:val=\"center\"/><w:tcMargin w:left=\"200\"/></w:tcPr>")
                    field("Category", photo.category.displayName)
                    field("Caption", photo.caption)
                    append("<w:p/>")

                    append("<w:p><w:r><w:rPr><w:b/></w:rPr><w:t>GPS METADATA</w:t></w:r></w:p>")
                    val timeStr = try {
                        val dt = kotlinx.datetime.Instant.fromEpochMilliseconds(photo.timestamp)
                            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                        "${dt.date} ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
                    } catch (e: Exception) {
                        "${photo.timestamp}"
                    }
                    field("Date/Time", timeStr)
                    field("Latitude", formatCoord(photo.latitude))
                    field("Longitude", formatCoord(photo.longitude))
                    field("Accuracy", "${photo.accuracy}m")
                    if (photo.address != null) {
                        field("Address", photo.address ?: "")
                    }

                    append("</w:tc>")

                    append("</w:tr>")
                    append("</w:tbl>")

                    append("<w:p/>")
                }
            }
            append("<w:sectPr><w:headerReference w:type=\"default\" r:id=\"rIdHeader\"/><w:footerReference w:type=\"default\" r:id=\"rIdFooter\"/></w:sectPr>")
            append("</w:body>\n</w:document>\n")
        }

        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Default Extension="jpg" ContentType="image/jpeg"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/header1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/>
  <Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>
</Types>"""

        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        val wordRelsXml = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
            append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n")
            append("  <Relationship Id=\"rIdHeader\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/header\" Target=\"header1.xml\"/>\n")
            append("  <Relationship Id=\"rIdFooter\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer\" Target=\"footer1.xml\"/>\n")
            for (img in images) {
                append("  <Relationship Id=\"${img.relId}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/${img.fileName}\"/>\n")
            }
            append("</Relationships>")
        }

        val headerXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:hdr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
  <w:p>
    <w:pPr><w:jc w:val="center"/></w:pPr>
    <w:r>
      <w:drawing>
        <wp:inline distT="0" distB="0" distL="0" distR="0">
          <wp:extent cx="5715000" cy="598485"/>
          <wp:effectExtent l="0" t="0" r="0" b="0"/>
          <wp:docPr id="999" name="Picture 999"/>
          <wp:cNvGraphicFramePr><a:graphicFrameLocks noChangeAspect="1"/></wp:cNvGraphicFramePr>
          <a:graphic>
            <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
              <pic:pic>
                <pic:nvPicPr><pic:cNvPr id="999" name="Header.jpg"/><pic:cNvPicPr/></pic:nvPicPr>
                <pic:blipFill><a:blip r:embed="rIdHeaderImg"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>
                <pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="5715000" cy="598485"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom><a:ln><a:noFill/></a:ln></pic:spPr>
              </pic:pic>
            </a:graphicData>
          </a:graphic>
        </wp:inline>
      </w:drawing>
    </w:r>
  </w:p>
</w:hdr>"""

        val footerXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:ftr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:p>
    <w:pPr>
      <w:tabs><w:tab w:val="right" w:pos="9360"/></w:tabs>
    </w:pPr>
    <w:r><w:t xml:space="preserve">Borrower: ${visit.customerName ?: ""} </w:t></w:r>
    <w:r><w:tab/></w:r>
    <w:r><w:t xml:space="preserve">Page </w:t></w:r>
    <w:r><w:fldChar w:fldCharType="begin"/></w:r>
    <w:r><w:instrText xml:space="preserve"> PAGE </w:instrText></w:r>
    <w:r><w:fldChar w:fldCharType="end"/></w:r>
  </w:p>
</w:ftr>"""

        val headerRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdHeaderImg" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/header.jpg"/>
</Relationships>"""


        val mediaEntries = mutableMapOf<String, ByteArray>()
        mediaEntries["word/media/header.jpg"] = PnbHeaderImageBytes
        for (img in images) {
            mediaEntries["word/media/${img.fileName}"] = img.bytes
        }

        val entries = mutableMapOf<String, ByteArray>()
        entries["[Content_Types].xml"] = contentTypesXml.encodeToByteArray()
        entries["_rels/.rels"] = relsXml.encodeToByteArray()
        entries["word/_rels/document.xml.rels"] = wordRelsXml.encodeToByteArray()
        entries["word/_rels/header1.xml.rels"] = headerRelsXml.encodeToByteArray()
        entries["word/document.xml"] = documentXml.encodeToByteArray()
        entries["word/header1.xml"] = headerXml.encodeToByteArray()
        entries["word/footer1.xml"] = footerXml.encodeToByteArray()
        entries.putAll(mediaEntries)

        return buildDocxZip(entries)
    }

    // ------------------------------------------------------------------ helpers

    private class ImageData(
        val relId: String,
        val fileName: String,
        val id: Int,
        val emuWidthPx: Int,
        val emuHeightPx: Int,
        val bytes: ByteArray,
    )

    private fun imgToEmu(width: Int, height: Int): Pair<Int, Int> {
        // Fit inside roughly a 65%-width column of an A4 page, bounded strictly
        // by height to guarantee 2 photos can easily fit on a single printed page.
        val maxPxW = 450
        val maxPxH = 300
        val scaleW = maxPxW.toDouble() / width
        val scaleH = maxPxH.toDouble() / height
        val scale = minOf(scaleW, scaleH, 1.0)
        return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
    }

    private fun imageDrawing(relId: String, docId: Int, cxEmu: Long, cyEmu: Long): String {
        return (
            "<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r>" +
                "<w:drawing>" +
                "<wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">" +
                "<wp:extent cx=\"$cxEmu\" cy=\"$cyEmu\"/>" +
                "<wp:effectExtent l=\"0\" t=\"0\" r=\"0\" b=\"0\"/>" +
                "<wp:docPr id=\"$docId\" name=\"Picture $docId\"/>" +
                "<wp:cNvGraphicFramePr><a:graphicFrameLocks noChangeAspect=\"1\"/></wp:cNvGraphicFramePr>" +
                "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">" +
                "<pic:pic>" +
                "<pic:nvPicPr><pic:cNvPr id=\"$docId\" name=\"Image $docId.jpg\"/><pic:cNvPicPr/></pic:nvPicPr>" +
                "<pic:blipFill><a:blip r:embed=\"$relId\"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>" +
                "<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"$cxEmu\" cy=\"$cyEmu\"/></a:xfrm>" +
                "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom>" +
                "<a:ln><a:noFill/></a:ln>" +
                "</pic:spPr>" +
                "</pic:pic>" +
                "</a:graphicData></a:graphic>" +
                "</wp:inline>" +
                "</w:drawing>" +
                "</w:r></w:p>\n"
            )
    }

    private fun StringBuilder.heading(text: String) {
        append("<w:p><w:pPr><w:pStyle w:val=\"Heading1\"/><w:spacing w:before=\"240\" w:after=\"120\"/></w:pPr>")
        append("<w:r><w:rPr><w:b/><w:sz w:val=\"28\"/></w:rPr><w:t xml:space=\"preserve\">${esc(text)}</w:t></w:r></w:p>\n")
    }

    private fun StringBuilder.field(label: String, value: String) {
        append("<w:p><w:pPr><w:spacing w:after=\"0\"/></w:pPr><w:r><w:rPr><w:b/></w:rPr><w:t xml:space=\"preserve\">${esc(label)}: </w:t></w:r>")
        append("<w:r><w:t xml:space=\"preserve\">${esc(value)}</w:t></w:r></w:p>\n")
    }

    private fun StringBuilder.para(text: String) {
        append("<w:p><w:r><w:t xml:space=\"preserve\">${esc(text)}</w:t></w:r></w:p>\n")
    }

    private fun StringBuilder.line() {
        append("<w:p><w:pPr><w:pBdr><w:bottom w:val=\"single\" w:sz=\"6\" w:space=\"1\" w:color=\"auto\"/></w:pBdr></w:pPr></w:p>\n")
    }

    private fun StringBuilder.pageBreak() {
        append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>\n")
    }

    private fun esc(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private const val EMU_PER_PX: Long = 9525L
}