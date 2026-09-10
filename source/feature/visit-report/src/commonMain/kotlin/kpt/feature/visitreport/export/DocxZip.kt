package kpt.feature.visitreport.export

internal fun buildDocxZip(entries: Map<String, ByteArray>): ByteArray {
    val buf = DocxZipBuilder()
    entries.forEach { (name, content) ->
        buf.addEntry(name, content)
    }
    return buf.finish()
}

private class DocxZipBuilder {
    private val centralDir = mutableListOf<ByteArray>()
    private val body = mutableListOf<ByteArray>()
    private var offset = 0

    fun addEntry(name: String, data: ByteArray) {
        val nameBytes = name.encodeToByteArray()
        val localHeader = localFileHeader(nameBytes, data)
        body.add(localHeader)
        body.add(data)
        centralDir.add(centralDirEntry(nameBytes, data, offset))
        offset += localHeader.size + data.size
    }

    fun finish(): ByteArray {
        val centralStart = offset
        var centralSize = 0
        centralDir.forEach { centralSize += it.size }
        val out = mutableListOf<ByteArray>()
        out.addAll(body)
        out.addAll(centralDir)
        out.add(endOfCentralDir(centralDir.size, centralSize, centralStart))
        return out.fold(ByteArray(0)) { acc, b -> acc + b }
    }

    private fun localFileHeader(name: ByteArray, data: ByteArray): ByteArray {
        val header = ByteArray(30 + name.size)
        putInt(header, 0, 0x04034b50)      // local file header sig
        putShort(header, 4, 20)             // version needed
        putShort(header, 6, 0)              // flags
        putShort(header, 8, 0)              // compression: stored
        putShort(header, 10, 0)             // mod time
        putShort(header, 12, 0)             // mod date
        putInt(header, 14, crc32(data))     // crc-32
        putInt(header, 18, data.size)       // compressed size
        putInt(header, 22, data.size)       // uncompressed size
        putShort(header, 26, name.size)     // name length
        putShort(header, 28, 0)             // extra field length
        name.copyInto(header, 30)
        return header
    }

    private fun centralDirEntry(name: ByteArray, data: ByteArray, localOffset: Int): ByteArray {
        val entry = ByteArray(46 + name.size)
        putInt(entry, 0, 0x02014b50)        // central dir sig
        putShort(entry, 4, 20)              // version made by
        putShort(entry, 6, 20)              // version needed
        putShort(entry, 8, 0)               // flags
        putShort(entry, 10, 0)              // compression: stored
        putShort(entry, 12, 0)              // mod time
        putShort(entry, 14, 0)              // mod date
        putInt(entry, 16, crc32(data))      // crc-32
        putInt(entry, 20, data.size)        // compressed size
        putInt(entry, 24, data.size)        // uncompressed size
        putShort(entry, 28, name.size)      // name length
        putShort(entry, 30, 0)              // extra field length
        putShort(entry, 32, 0)              // comment length
        putShort(entry, 34, 0)              // disk number start
        putShort(entry, 36, 0)              // internal attrs
        putInt(entry, 38, 0)                // external attrs
        putInt(entry, 42, localOffset)      // relative offset
        name.copyInto(entry, 46)
        return entry
    }

    private fun endOfCentralDir(count: Int, size: Int, offset: Int): ByteArray {
        val eocd = ByteArray(22)
        putInt(eocd, 0, 0x06054b50)         // end of central dir sig
        putShort(eocd, 4, 0)                // disk number
        putShort(eocd, 6, 0)                // disk number with central dir
        putShort(eocd, 8, count)            // entries on this disk
        putShort(eocd, 10, count)           // total entries
        putInt(eocd, 12, size)              // size of central dir
        putInt(eocd, 16, offset)            // offset of central dir
        putShort(eocd, 20, 0)              // comment length
        return eocd
    }

    private fun putShort(buf: ByteArray, off: Int, value: Int) {
        buf[off] = (value and 0xFF).toByte()
        buf[off + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun putInt(buf: ByteArray, off: Int, value: Int) {
        buf[off] = (value and 0xFF).toByte()
        buf[off + 1] = ((value shr 8) and 0xFF).toByte()
        buf[off + 2] = ((value shr 16) and 0xFF).toByte()
        buf[off + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun crc32(data: ByteArray): Int {
        var crc = 0xFFFFFFFF.toInt()
        for (b in data) {
            crc = crc xor (b.toInt() and 0xFF)
            repeat(8) {
                crc = if (crc and 1 != 0) (crc ushr 1) xor 0xEDB88320.toInt() else crc ushr 1
            }
        }
        return crc xor 0xFFFFFFFF.toInt()
    }
}
