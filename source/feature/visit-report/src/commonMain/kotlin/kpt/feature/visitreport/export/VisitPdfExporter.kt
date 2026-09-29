package kpt.feature.visitreport.export

import kpt.core.model.visit.Visit
import kpt.core.model.visit.VisitPhoto
import kotlin.math.max
import kotlinx.datetime.toLocalDateTime

object VisitPdfExporter {

    /**
     * Generates a multi-page A4 PDF report for [visit].
     *
     * @param photoBytes resolves a [VisitPhoto] path/URI into raw image bytes so
     * the actual photograph is embedded (JPEG / DCTDecode). When it returns
     * `null`, a graceful placeholder frame is drawn instead.
     */
    fun generatePdfBytes(visit: Visit, photoBytes: (VisitPhoto) -> ByteArray?): ByteArray {
        var objId = 1
        fun nextId() = objId++

        val catalogId = nextId()
        val pagesId = nextId()

        val pageIds = mutableListOf<Int>()

        // --- Content Building Helper ---
        class PdfPage {
            val content = StringBuilder()
            var yPos = 780

            fun addText(x: Int, y: Int, text: String, fontSize: Int = 12, isBold: Boolean = false) {
                val fontRef = if (isBold) "/F2" else "/F1"
                content.append("BT $fontRef $fontSize Tf $x $y Td (${esc(text)}) Tj ET\n")
            }

            fun addRect(x: Int, y: Int, w: Int, h: Int) {
                content.append("$x $y $w $h re S\n")
            }

            fun fillRect(x: Int, y: Int, w: Int, h: Int) {
                content.append("$x $y $w $h re f\n")
            }

            fun addLine(x1: Int, y1: Int, x2: Int, y2: Int) {
                content.append("$x1 $y1 m $x2 $y2 l S\n")
            }
        }



        // --- Layout Parameters ---
        val marginX = 50
        val contentWidth = 495

        // --- Images captured during layout, emitted as /XObject DCTDecode ---
        val imageStore = mutableMapOf<String, ByteArray>()
        val imageDims = mutableMapOf<String, Pair<Int, Int>>()
        fun putImage(name: String, bytes: ByteArray, w: Int, h: Int) {
            if (name !in imageStore) imageStore[name] = bytes
            imageDims[name] = w to h
        }

        var currentPage = PdfPage()
        val pages = mutableListOf<PdfPage>()
                var pageCount = 0

        fun newPage() {
            currentPage = PdfPage()
            pageCount++
            
            // Header Image
            // We want it at the top. Top margin is 842. yPos starts at 780.
            val headerH = 52
            val headerW = 495
            val headerY = 780
            
            putImage("PNBHeader", PnbHeaderImageBytes, PnbHeaderImageWidth, PnbHeaderImageHeight)
            currentPage.content.append("q $headerW 0 0 $headerH $marginX $headerY cm /PNBHeader Do Q\n")
            
            // Footer
            val footerY = 30
            val borrowerStr = "Borrower: ${visit.customerName}"
            val pageStr = "Page $pageCount"
            currentPage.addText(marginX, footerY, borrowerStr, 10, true)
            currentPage.addText(marginX + contentWidth - 40, footerY, pageStr, 10, true)
            
            currentPage.yPos = headerY - 30 // Start content below the header
            pages.add(currentPage)
        }
        
        newPage() // Initialize first page

        currentPage.yPos -= 20
        fun drawKeyValue(label: String, value: String) {
            if (currentPage.yPos < 60) newPage()
            currentPage.addText(marginX, currentPage.yPos, label, 11, true)
            currentPage.addText(marginX + 150, currentPage.yPos, value, 11, false)
            currentPage.yPos -= 18
        }

        fun drawLongText(title: String, text: String) {
            if (currentPage.yPos < 100) newPage()
            currentPage.addText(marginX, currentPage.yPos, title, 14, true)
            currentPage.yPos -= 20
            currentPage.addLine(marginX, currentPage.yPos + 10, marginX + contentWidth, currentPage.yPos + 10)

            val words = text.split(" ")
            var line = ""
            for (word in words) {
                if (line.length + word.length > 80) {
                    if (currentPage.yPos < 60) newPage()
                    currentPage.addText(marginX, currentPage.yPos, line, 10, false)
                    currentPage.yPos -= 14
                    line = ""
                }
                line += "$word "
            }
            if (line.isNotEmpty()) {
                if (currentPage.yPos < 60) newPage()
                currentPage.addText(marginX, currentPage.yPos, line, 10, false)
                currentPage.yPos -= 14
            }
            currentPage.yPos -= 10
        }

        if (visit.reportFormat == kpt.core.model.visit.ReportFormat.GENERAL) {
            // --- 1. VISIT / CUSTOMER DETAILS ---
            currentPage.addText(marginX, currentPage.yPos, "1. VISIT / CUSTOMER DETAILS", 14, true)
            currentPage.yPos -= 20
            currentPage.addLine(marginX, currentPage.yPos + 10, marginX + contentWidth, currentPage.yPos + 10)

            drawKeyValue("Customer Name:", visit.customerName)
            drawKeyValue("CIF / Customer ID:", visit.referenceNumber ?: "N/A")
            drawKeyValue("Loan Account No.:", visit.loanAccountNumber)
            drawKeyValue("Address:", visit.address)
            drawKeyValue("Visit Date:", visit.visitDate)
            drawKeyValue("Visit Time:", visit.visitTime)
            drawKeyValue("Officer:", "${visit.officerName} (${visit.branch})")
            currentPage.yPos -= 10

            // --- 2. PURPOSE / VISIT DETAILS ---
            if (currentPage.yPos < 100) newPage()
            currentPage.addText(marginX, currentPage.yPos, "2. PURPOSE / VISIT DETAILS", 14, true)
            currentPage.yPos -= 20
            currentPage.addLine(marginX, currentPage.yPos + 10, marginX + contentWidth, currentPage.yPos + 10)
            drawKeyValue("Purpose of Visit:", visit.purpose)
            drawKeyValue("Visit Type:", visit.status.name)
            currentPage.yPos -= 10

            // --- 3. OFFICER OBSERVATIONS ---
            drawLongText("3. OFFICER OBSERVATIONS", visit.observations)

            // --- 4. RECOMMENDATIONS ---
            drawLongText("4. RECOMMENDATIONS", visit.recommendations)

            // --- 5. REMARKS / CONCLUSION ---
            drawLongText("5. REMARKS / CONCLUSION", visit.remarks)
        }

        fun drawTable(headers: List<String>, rows: List<List<String>>, colWeights: List<Float>?) {
            val numCols = headers.size
            val weights = colWeights ?: List(numCols) { 1.0f }
            val totalWeight = weights.sum()

            // Calculate column widths proportional to weights
            val colWidths = mutableListOf<Int>()
            var allocated = 0
            for (i in 0 until numCols - 1) {
                val w = ((weights[i] / totalWeight) * contentWidth).toInt()
                colWidths.add(w)
                allocated += w
            }
            colWidths.add(contentWidth - allocated)

            val rowHeight = 22
            val totalTableHeight = (1 + rows.size) * rowHeight + 15
            if (currentPage.yPos - totalTableHeight < 60) {
                newPage()
            }

            val tableTop = currentPage.yPos
            val tableBottom = tableTop - (1 + rows.size) * rowHeight

            // Draw header background
            currentPage.content.append("0.93 0.93 0.93 rg\n")
            currentPage.fillRect(marginX, tableTop - rowHeight, contentWidth, rowHeight)
            currentPage.content.append("0 0 0 rg\n")

            // Header text
            var curX = marginX
            for (i in 0 until numCols) {
                val colW = colWidths[i]
                currentPage.addText(curX + 6, tableTop - 15, headers[i], 9, isBold = true)
                curX += colW
            }

            // Data rows text
            var curY = tableTop - rowHeight
            for (row in rows) {
                curX = marginX
                val isTotalRow = row.firstOrNull()?.contains("TOTAL", ignoreCase = true) == true
                for (i in 0 until numCols) {
                    val colW = colWidths[i]
                    val cellText = if (i < row.size) row[i] else ""
                    currentPage.addText(curX + 6, curY - 15, cellText, 9, isBold = isTotalRow)
                    curX += colW
                }
                curY -= rowHeight
            }

            // Draw horizontal grid lines
            for (r in 0..(1 + rows.size)) {
                val lineY = tableTop - r * rowHeight
                currentPage.addLine(marginX, lineY, marginX + contentWidth, lineY)
            }

            // Draw vertical grid lines
            var vertX = marginX
            currentPage.addLine(vertX, tableTop, vertX, tableBottom)
            for (i in 0 until numCols) {
                vertX += colWidths[i]
                currentPage.addLine(vertX, tableTop, vertX, tableBottom)
            }

            currentPage.yPos = tableBottom - 15
        }

        // --- PROFORMA FORMAT ---
        if (visit.reportFormat != kpt.core.model.visit.ReportFormat.GENERAL) {
            ProformaTemplates.injectProformaFields(
                visit = visit,
                drawHeading = { title ->
                    if (currentPage.yPos < 100) newPage()
                    currentPage.addText(marginX, currentPage.yPos - 10, title, 14, true)
                    currentPage.yPos -= 30
                    currentPage.addLine(marginX, currentPage.yPos + 10, marginX + contentWidth, currentPage.yPos + 10)
                },
                drawField = { key, value ->
                    drawKeyValue(key, value)
                },
                drawTable = { headers, rows, colWeights ->
                    drawTable(headers, rows, colWeights)
                }
            )
            currentPage.yPos -= 10
        }

        // --- 6. FIELD PHOTOGRAPHS ---
        if (visit.photos.isNotEmpty()) {
            if (currentPage.yPos < 150) newPage()
            currentPage.addText(marginX, currentPage.yPos, "6. FIELD PHOTOGRAPHS", 14, true)
            currentPage.yPos -= 20
            currentPage.addLine(marginX, currentPage.yPos + 10, marginX + contentWidth, currentPage.yPos + 10)

            for ((idx, photo) in visit.photos.withIndex()) {
                // Each block needs ~300 units; break early when it won't fit.
                if (currentPage.yPos < 320) newPage()

                currentPage.addText(marginX, currentPage.yPos, "Photograph ${idx + 1}", 12, true)
                currentPage.yPos -= 18

                // 65/35 split: photo column ~322, metadata column ~155
                val photoBoxW = 322
                val photoBoxH = 220
                val photoBoxX = marginX
                val photoBoxY = currentPage.yPos - photoBoxH + 10

                var embedded = false
                val rawBytes = photoBytes(photo)?.takeIf { it.isNotEmpty() }
                if (rawBytes != null) {
                    val dims = jpegDimensions(rawBytes)
                    if (dims != null) {
                        val (imgW, imgH) = dims
                        if (imgW in 2..10000 && imgH in 2..10000) {
                            val imageName = "Im${idx + 1}"
                            putImage(imageName, rawBytes, imgW, imgH)
                            // Add border frame around the photo
                            currentPage.addRect(photoBoxX, photoBoxY, photoBoxW, photoBoxH)

                            // Scale the image inside the frame, preserving aspect ratio
                            val scale = minOf(photoBoxW.toDouble() / imgW, photoBoxH.toDouble() / imgH)
                            val drawW = (imgW * scale).toInt().coerceAtLeast(1)
                            val drawH = (imgH * scale).toInt().coerceAtLeast(1)
                            val drawX = photoBoxX + (photoBoxW - drawW) / 2
                            val drawTop = currentPage.yPos
                            val drawY = drawTop - drawH

                            currentPage.content.append("q\n")
                            currentPage.content.append("$drawW 0 0 $drawH $drawX $drawY cm\n")
                            currentPage.content.append("/$imageName Do\nQ\n")
                            embedded = true
                        }
                    }
                }

                if (!embedded) {
                    // Graceful fallback: a professional placeholder frame.
                    currentPage.addRect(photoBoxX, photoBoxY, photoBoxW, photoBoxH)
                    currentPage.content.append("0.92 0.92 0.92 rg\n")
                    currentPage.fillRect(photoBoxX + 3, photoBoxY + 3, photoBoxW - 6, photoBoxH - 6)
                    currentPage.content.append("0 0 0 rg\n")
                    currentPage.addText(photoBoxX + 100, photoBoxY + photoBoxH / 2 + 8, "PHOTO NOT AVAILABLE", 9, true)
                    currentPage.addText(photoBoxX + 82, photoBoxY + photoBoxH / 2 - 8, "Image file could not be loaded", 7, false)
                }

                // Meta details next to photo (35% column)
                var metaY = currentPage.yPos
                val metaX = marginX + photoBoxW + 15
                val metaColWidth = marginX + contentWidth - metaX

                currentPage.addText(metaX, metaY, "Category:", 10, true)
                currentPage.addText(metaX + 60, metaY, photo.category.displayName, 10, false)
                metaY -= 16

                currentPage.addText(metaX, metaY, "Caption:", 10, true)
                currentPage.addText(metaX + 60, metaY, photo.caption.take(40), 10, false)
                metaY -= 16

                metaY -= 10
                currentPage.addText(metaX, metaY, "GPS METADATA", 10, true)
                metaY -= 16

                currentPage.addText(metaX, metaY, "Date/Time:", 10, true)
                val timeStr = try {
                    val dt = kotlinx.datetime.Instant.fromEpochMilliseconds(photo.timestamp)
                        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                    "${dt.date} ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
                } catch (e: Exception) {
                    "${photo.timestamp}"
                }
                currentPage.addText(metaX + 60, metaY, timeStr, 10, false)
                metaY -= 16

                currentPage.addText(metaX, metaY, "Latitude:", 10, true)
                currentPage.addText(metaX + 60, metaY, formatCoord(photo.latitude), 10, false)
                metaY -= 16

                currentPage.addText(metaX, metaY, "Longitude:", 10, true)
                currentPage.addText(metaX + 60, metaY, formatCoord(photo.longitude), 10, false)
                metaY -= 16

                currentPage.addText(metaX, metaY, "Accuracy:", 10, true)
                currentPage.addText(metaX + 60, metaY, "${photo.accuracy}m", 10, false)
                metaY -= 16

                val address = photo.address
                if (address != null) {
                    currentPage.addText(metaX, metaY, "Address:", 10, true)
                    metaY -= 16
                    val addrWords = address.split(" ")
                    var addrLine = ""
                    for (w in addrWords) {
                        if (addrLine.length + w.length > 35) {
                            currentPage.addText(metaX + 10, metaY, addrLine, 9, false)
                            metaY -= 12
                            addrLine = ""
                        }
                        addrLine += "$w "
                    }
                    if (addrLine.isNotEmpty()) {
                        currentPage.addText(metaX + 10, metaY, addrLine, 9, false)
                        metaY -= 12
                    }
                }

                currentPage.yPos = minOf(photoBoxY, metaY) - 30
                currentPage.addLine(marginX, currentPage.yPos + 15, marginX + contentWidth, currentPage.yPos + 15)
            }
        }

        // --- Final PDF Assembly ---
        val builtObjects = mutableMapOf<Int, ByteArray>()

        fun renderObject(bytes: ByteArray) {
            // Object headers are pure ASCII; decodeToString() (UTF-8) is lossless for them.
            val id = bytes.decodeToString().substringBefore(" 0 obj").toInt()
            builtObjects[id] = bytes
        }

        // Pages + content streams
        for (page in pages) {
            val pageId = nextId()
            pageIds.add(pageId)
            val contentId = nextId()

            val contentBytes = page.content.toString().encodeToByteArray()
            renderObject(
                "$pageId 0 obj\n<< /Type /Page /Parent $pagesId 0 R /MediaBox [0 0 595 842] /Contents $contentId 0 R >>\nendobj\n".encodeToByteArray(),
            )
            val streamHead = "$contentId 0 obj\n<< /Length ${contentBytes.size} >>\nstream\n".encodeToByteArray()
            val streamTail = "endstream\nendobj\n".encodeToByteArray()
            val combined = headTailConcat(streamHead, contentBytes, streamTail)
            renderObject(combined)
        }

        // Image XObjects (DCTDecode JPEG)
        val imageResourceRefs = mutableMapOf<String, Int>()
        for (name in imageStore.keys) {
            val imageId = nextId()
            val bytes = imageStore.getValue(name)
            val (w, h) = imageDims.getValue(name)
            imageResourceRefs[name] = imageId
            val head =
                "$imageId 0 obj\n<< /Type /XObject /Subtype /Image /Width $w /Height $h /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${bytes.size} >>\nstream\n".encodeToByteArray()
            renderObject(headTailConcat(head, bytes, "endstream\nendobj\n".encodeToByteArray()))
        }

        val font1Id = nextId()
        val font2Id = nextId()

        // Pages dict
        val kidsStr = pageIds.joinToString(" ") { "$it 0 R" }
        val fontRes = "/Font << /F1 $font1Id 0 R /F2 $font2Id 0 R >>"
        val xobjRes = if (imageResourceRefs.isNotEmpty()) {
            " /XObject << " + imageResourceRefs.entries.joinToString(" ") { "/${it.key} ${it.value} 0 R" } + " >>"
        } else {
            ""
        }
        renderObject(
            "$pagesId 0 obj\n<< /Type /Pages /Count ${pageIds.size} /Kids [$kidsStr] /Resources << $fontRes$xobjRes >> >>\nendobj\n".encodeToByteArray(),
        )

        // Fonts
        renderObject("$font1Id 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n".encodeToByteArray())
        renderObject("$font2Id 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n".encodeToByteArray())

        // Catalog
        renderObject("$catalogId 0 obj\n<< /Type /Catalog /Pages $pagesId 0 R >>\nendobj\n".encodeToByteArray())

        // Write out using the actual object IDs
        val pdf = ByteArrayBuilder()
        pdf.appendText("%PDF-1.4\n")
        val offsets = mutableListOf<Int>()
        offsets.add(0)
        for (i in 1 until objId) {
            offsets.add(pdf.size())
            pdf.appendBytes(builtObjects[i] ?: "$i 0 obj\nendobj\n".encodeToByteArray())
        }
        val xrefStart = pdf.size()
        pdf.appendText("xref\n0 $objId\n")
        pdf.appendText("0000000000 65535 f \n")
        for (i in 1 until objId) {
            pdf.appendText(offsets[i].toString().padStart(10, '0') + " 00000 n \n")
        }
        pdf.appendText("trailer\n<< /Size $objId /Root $catalogId 0 R >>\n")
        pdf.appendText("startxref\n$xrefStart\n%%EOF\n")
        return pdf.toByteArray()
    }

    private fun headTailConcat(head: ByteArray, body: ByteArray, tail: ByteArray): ByteArray {
        val out = ByteArray(head.size + body.size + tail.size)
        head.copyInto(out, 0)
        body.copyInto(out, head.size)
        tail.copyInto(out, head.size + body.size)
        return out
    }

    /** Minimal append-only byte buffer (avoids repeated list reallocation). */
    private class ByteArrayBuilder {
        private var data = ByteArray(8192)
        private var size = 0

        fun size(): Int = size

        fun appendBytes(bytes: ByteArray) {
            ensureCapacity(size + bytes.size)
            bytes.copyInto(data, size)
            size += bytes.size
        }

        fun appendText(text: String) = appendBytes(text.encodeToByteArray())

        private fun ensureCapacity(needed: Int) {
            if (needed <= data.size) return
            var newSize = data.size
            while (newSize < needed) newSize *= 2
            data = data.copyOf(newSize)
        }

        fun toByteArray(): ByteArray = data.copyOf(size)
    }

    private fun esc(s: String) = s.replace("(", "\\(").replace(")", "\\)")
}