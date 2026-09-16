package com.tumesh007.pdfredactor

import com.tom_roush.pdfbox.cos.COSArray
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.cos.COSStream
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Stack
import java.util.regex.Pattern

data class WatermarkSignature(
    val literals: List<String> = emptyList(),
    val patterns: List<String> = WatermarkDefaults.defaultPatterns
) {
    private val compiledPatterns = patterns.mapNotNull {
        try { Pattern.compile(it, Pattern.CASE_INSENSITIVE) } catch (_: Exception) { null }
    }

    fun matches(text: String): Boolean {
        if (text.isBlank()) return false
        val trimmed = text.trim()
        if (literals.any { lit -> lit.isNotBlank() && trimmed.contains(lit, ignoreCase = true) }) {
            return true
        }
        return compiledPatterns.any { it.matcher(trimmed).find() }
    }
}

data class WatermarkRemovalResult(
    val sourcePath: File? = null,
    val outputPath: File? = null,
    val pagesTotal: Int = 0,
    val pagesCleaned: Int = 0,
    val pagesViaSeparateStream: Int = 0,
    val pagesViaInlineBlock: Int = 0,
    val pagesUnmatched: List<Int> = emptyList(),
    val success: Boolean = false,
    val error: String? = null
)

object WatermarkRemover {

    fun removeWatermark(
        inputFile: File,
        outputFile: File,
        signature: WatermarkSignature = WatermarkDefaults.createSignature(),
        password: String? = null
    ): WatermarkRemovalResult {
        return try {
            val doc = if (password != null) PDDocument.load(inputFile, password) else PDDocument.load(inputFile)
            doc.use { document ->
                val result = processDocument(document, signature, inputFile, outputFile)
                outputFile.parentFile?.mkdirs()
                document.save(outputFile)
                result
            }
        } catch (e: Exception) {
            WatermarkRemovalResult(
                sourcePath = inputFile,
                outputPath = outputFile,
                success = false,
                error = e.message ?: "Failed to remove watermark"
            )
        }
    }

    fun removeWatermarkStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        signature: WatermarkSignature = WatermarkDefaults.createSignature(),
        password: String? = null
    ): WatermarkRemovalResult {
        return try {
            val doc = if (password != null) PDDocument.load(inputStream, password) else PDDocument.load(inputStream)
            doc.use { document ->
                val result = processDocument(document, signature, null, null)
                document.save(outputStream)
                result
            }
        } catch (e: Exception) {
            WatermarkRemovalResult(
                sourcePath = null,
                outputPath = null,
                success = false,
                error = e.message ?: "Failed to remove watermark"
            )
        }
    }

    private fun processDocument(
        document: PDDocument,
        signature: WatermarkSignature,
        inputFile: File?,
        outputFile: File?
    ): WatermarkRemovalResult {
        val totalPages = document.numberOfPages
        var pagesCleaned = 0
        var pagesViaSeparateStream = 0
        var pagesViaInlineBlock = 0
        val unmatched = mutableListOf<Int>()

        for (pno in 0 until totalPages) {
            val page = document.getPage(pno)
            if (stripViaSeparateStream(page, signature)) {
                pagesCleaned++
                pagesViaSeparateStream++
                continue
            }
            if (stripViaInlineBlock(document, page, signature)) {
                pagesCleaned++
                pagesViaInlineBlock++
                continue
            }
            unmatched.add(pno + 1)
        }

        return WatermarkRemovalResult(
            sourcePath = inputFile,
            outputPath = outputFile,
            pagesTotal = totalPages,
            pagesCleaned = pagesCleaned,
            pagesViaSeparateStream = pagesViaSeparateStream,
            pagesViaInlineBlock = pagesViaInlineBlock,
            pagesUnmatched = unmatched,
            success = true
        )
    }

    private fun stripViaSeparateStream(
        page: PDPage,
        signature: WatermarkSignature
    ): Boolean {
        val cosObject = page.cosObject
        val contentsObj = cosObject.getDictionaryObject(COSName.CONTENTS) ?: return false
        if (contentsObj !is COSArray) return false

        val array = contentsObj
        if (array.size() <= 1) return false

        val streamsList = page.contentStreams.asSequence().toList()
        if (streamsList.size != array.size()) return false

        val keepIndices = mutableListOf<Int>()
        var foundWatermarkStream = false

        for (i in streamsList.indices) {
            val streamObj = streamsList[i]
            val rawBytes = streamObj.toByteArray()
            val texts = extractTextLiterals(rawBytes)
            val nonBlankTexts = texts.filter { it.isNotBlank() }

            if (nonBlankTexts.isNotEmpty() && nonBlankTexts.all { signature.matches(it) }) {
                foundWatermarkStream = true
            } else {
                keepIndices.add(i)
            }
        }

        if (foundWatermarkStream && keepIndices.isNotEmpty()) {
            val newArray = COSArray()
            for (idx in keepIndices) {
                newArray.add(array.get(idx))
            }
            cosObject.setItem(COSName.CONTENTS, newArray)
            return true
        }
        return false
    }

    private fun stripViaInlineBlock(
        document: PDDocument,
        page: PDPage,
        signature: WatermarkSignature
    ): Boolean {
        val rawBytes = try {
            val os = ByteArrayOutputStream()
            page.contentStreams.forEach { stream ->
                os.write(stream.toByteArray())
            }
            os.toByteArray()
        } catch (_: Exception) {
            return false
        }

        if (rawBytes.isEmpty()) return false
        val contentStr = String(rawBytes, Charsets.ISO_8859_1)
        val masked = maskStrings(rawBytes)

        val spansToRemove = mutableListOf<IntRange>()

        val stringLiteralRegex = Pattern.compile("""\((?:[^()\\]|\\.)*\)|<[0-9A-Fa-f\s]*>""")
        val matcher = stringLiteralRegex.matcher(contentStr)

        while (matcher.find()) {
            val rawText = matcher.group() ?: continue
            val decoded = if (rawText.startsWith("(")) {
                decodePdfLiteral(rawText.substring(1, rawText.length - 1))
            } else if (rawText.startsWith("<") && rawText.endsWith(">")) {
                decodeHexLiteral(rawText.substring(1, rawText.length - 1))
            } else {
                ""
            }

            if (decoded.isNotBlank() && signature.matches(decoded)) {
                val matchStart = matcher.start()
                val span = findEnclosingQQSpan(masked, matchStart)
                if (span != null) {
                    spansToRemove.add(span)
                }
            }
        }

        if (spansToRemove.isEmpty()) return false

        val sortedSpans = spansToRemove.distinct().sortedByDescending { it.first }
        var currentBytes = rawBytes

        for (span in sortedSpans) {
            val start = span.first.coerceIn(0, currentBytes.size)
            val end = (span.last + 1).coerceIn(start, currentBytes.size)
            val left = currentBytes.copyOfRange(0, start)
            val right = currentBytes.copyOfRange(end, currentBytes.size)
            currentBytes = left + right
        }

        val cosObject = page.cosObject
        val newStream = COSStream()
        val os = newStream.createOutputStream()
        os.write(currentBytes)
        os.close()

        cosObject.setItem(COSName.CONTENTS, newStream)
        return true
    }

    fun extractTextLiterals(raw: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val str = String(raw, Charsets.ISO_8859_1)

        val parenRegex = Pattern.compile("""\((?:[^()\\]|\\.)*\)""")
        val parenMatcher = parenRegex.matcher(str)
        while (parenMatcher.find()) {
            val match = parenMatcher.group()
            if (match.length >= 2) {
                val body = match.substring(1, match.length - 1)
                result.add(decodePdfLiteral(body))
            }
        }

        val hexRegex = Pattern.compile("""<([0-9A-Fa-f\s]+)>""")
        val hexMatcher = hexRegex.matcher(str)
        while (hexMatcher.find()) {
            val hexBody = hexMatcher.group(1) ?: ""
            val decoded = decodeHexLiteral(hexBody)
            if (decoded.isNotBlank()) {
                result.add(decoded)
            }
        }
        return result
    }

    private fun decodeHexLiteral(hexBody: String): String {
        val cleaned = hexBody.replace(Regex("""\s+"""), "")
        if (cleaned.length % 2 != 0 || cleaned.isEmpty()) return ""
        return try {
            val bytes = cleaned.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val ascii = String(bytes, Charsets.ISO_8859_1)
            if (bytes.size % 2 == 0) {
                val utf16 = String(bytes, Charsets.UTF_16BE)
                if (utf16.count { it.isLetterOrDigit() } > ascii.count { it.isLetterOrDigit() }) {
                    return utf16
                }
            }
            ascii
        } catch (_: Exception) {
            ""
        }
    }

    private fun decodePdfLiteral(raw: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '\\' && i + 1 < raw.length) {
                val next = raw[i + 1]
                when (next) {
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'b' -> { sb.append('\b'); i += 2 }
                    'f' -> { sb.append('\u000C'); i += 2 }
                    '(', ')', '\\' -> { sb.append(next); i += 2 }
                    in '0'..'7' -> {
                        var oct = next.toString()
                        var j = i + 2
                        while (j < raw.length && j < i + 4 && raw[j] in '0'..'7') {
                            oct += raw[j]
                            j++
                        }
                        sb.append(oct.toInt(8).toChar())
                        i = j
                    }
                    else -> { sb.append(next); i += 2 }
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun maskStrings(raw: ByteArray): String {
        val chars = String(raw, Charsets.ISO_8859_1).toCharArray()
        var inParen = false
        var depth = 0
        var escaped = false

        for (i in chars.indices) {
            val c = chars[i]
            if (escaped) {
                if (inParen) chars[i] = ' '
                escaped = false
                continue
            }
            if (c == '\\') {
                if (inParen) chars[i] = ' '
                escaped = true
                continue
            }
            if (c == '(') {
                depth++
                inParen = true
                chars[i] = ' '
                continue
            }
            if (c == ')') {
                depth--
                chars[i] = ' '
                if (depth <= 0) {
                    depth = 0
                    inParen = false
                }
                continue
            }
            if (inParen) {
                chars[i] = ' '
            }
        }
        return String(chars)
    }

    private fun findEnclosingQQSpan(masked: String, targetIdx: Int): IntRange? {
        val qStack = Stack<Int>()
        var enclosingQIdx: Int? = null
        var enclosingQEndIdx: Int? = null

        val tokenRegex = Pattern.compile("""(?<=\s|^)(q|Q)(?=\s|$)""")
        val matcher = tokenRegex.matcher(masked)

        while (matcher.find()) {
            val tok = matcher.group()
            val start = matcher.start()
            val end = matcher.end()

            if (tok == "q") {
                qStack.push(start)
            } else if (tok == "Q" && qStack.isNotEmpty()) {
                val qStart = qStack.pop()
                if (targetIdx in qStart..end) {
                    enclosingQIdx = qStart
                    enclosingQEndIdx = end
                    break
                }
            }
        }

        if (enclosingQIdx != null && enclosingQEndIdx != null) {
            return IntRange(enclosingQIdx, enclosingQEndIdx - 1)
        }
        return null
    }

    fun verifyContentUnchanged(originalFile: File, cleanedFile: File): Pair<Boolean, List<Int>> {
        return try {
            val origDoc = PDDocument.load(originalFile)
            val cleanDoc = PDDocument.load(cleanedFile)
            val mismatches = mutableListOf<Int>()

            val minPages = minOf(origDoc.numberOfPages, cleanDoc.numberOfPages)
            for (pno in 0 until minPages) {
                val origStream = origDoc.getPage(pno).contentStreams.next().toByteArray()
                val cleanStream = cleanDoc.getPage(pno).contentStreams.next().toByteArray()
                if (!origStream.contentEquals(cleanStream)) {
                    mismatches.add(pno + 1)
                }
            }
            origDoc.close()
            cleanDoc.close()
            Pair(mismatches.isEmpty(), mismatches)
        } catch (_: Exception) {
            Pair(false, emptyList())
        }
    }
}
