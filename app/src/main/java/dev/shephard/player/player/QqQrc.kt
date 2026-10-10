package dev.shephard.player.player

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.Inflater

/**
 * QQ Music word-by-word ("QRC") lyrics.
 *
 * `lyric_download.fcg` returns the karaoke lyrics QQ Music itself uses: a hex string encrypted
 * with QQ's own Triple-DES variant (the S-box tables differ from standard DES in a few entries,
 * so no stock cipher can decrypt it) and then zlib compressed. Inside is an XML document whose
 * `LyricContent` holds lines like `[12000,3500]Word(12000,400)word(12400,300)...`, i.e. a line
 * start/duration followed by every word (or Chinese character) with its own start/duration.
 *
 * The result is converted to enhanced LRC (`[mm:ss.xx]<mm:ss.xx>word<mm:ss.xx>word<mm:ss.xx>`)
 * so it can be stored, exported and re-parsed like any other lyrics text.
 *
 * The DES variant follows QQMusicDecoder (MIT, github.com/WXRIW/QQMusicDecoder), itself based
 * on Brad Conte's public-domain DES implementation.
 */
internal object QqQrc {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36 LambdaPlayer/2.5"
    private val QRC_KEY = "!@#)(*\$%123ZXC!@!@#)(NHL".toByteArray(Charsets.US_ASCII)

    /** Downloads the karaoke lyrics of [songId] and returns them as enhanced LRC, or null. */
    fun download(songId: Long): String? {
        val url = "https://c.y.qq.com/qqmusic/fcgi-bin/lyric_download.fcg" +
            "?version=15&miniversion=82&lrctype=4&musicid=$songId"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7_000
            readTimeout = 7_000
            requestMethod = "GET"
            setRequestProperty("Referer", "https://y.qq.com/")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            fromDownloadResponse(body)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    /** Extracts, decrypts and converts the `<content>` block of a `lyric_download.fcg` response. */
    fun fromDownloadResponse(body: String): String? {
        val hex = Regex("<content[^>]*>\\s*<!\\[CDATA\\[([0-9A-Fa-f]+)]]>").find(body)?.groupValues?.get(1)
            ?: return null
        if (hex.length < 16) return null
        val xml = runCatching { decrypt(hex) }.getOrNull() ?: return null
        val content = Regex("LyricContent=\"([\\s\\S]*?)\"\\s*/>").find(xml)?.groupValues?.get(1)
            ?.let(::decodeXmlEntities)
            ?: return null
        return qrcToEnhancedLrc(content)
    }

    /** Converts QRC line/word timings to enhanced LRC. Returns null if no word-timed line was found. */
    fun qrcToEnhancedLrc(qrc: String): String? {
        val lineRegex = Regex("^\\[(\\d+),(\\d+)](.*)$")
        val wordRegex = Regex("(.*?)\\((\\d+),(\\d+)\\)")
        val out = StringBuilder()
        var wordLines = 0
        qrc.replace("\r", "").lineSequence().forEach { raw ->
            val line = raw.trimEnd()
            val match = lineRegex.matchEntire(line)
            if (match == null) {
                // Keep metadata tags such as [ti:] / [ar:]; drop anything else.
                if (Regex("^\\[[A-Za-z]+:.*]$").matches(line)) out.append(line).append('\n')
                return@forEach
            }
            val lineStart = match.groupValues[1].toLong()
            val body = match.groupValues[3]
            val words = wordRegex.findAll(body).map { m ->
                Triple(m.groupValues[1], m.groupValues[2].toLong(), m.groupValues[3].toLong())
            }.filter { it.first.isNotEmpty() }.toList()
            if (words.isEmpty()) {
                val text = body.trim()
                if (text.isNotEmpty()) out.append('[').append(lrcTime(lineStart)).append(']').append(text).append('\n')
                return@forEach
            }
            if (words.joinToString("") { it.first }.isBlank()) return@forEach
            out.append('[').append(lrcTime(lineStart)).append(']')
            var end = lineStart
            for ((text, start, duration) in words) {
                out.append('<').append(lrcTime(start)).append('>').append(text)
                end = maxOf(end, start + duration)
            }
            // Closing tag = when the last word ends (needed to animate it).
            out.append('<').append(lrcTime(end)).append('>')
            out.append('\n')
            wordLines++
        }
        return out.toString().trim().takeIf { wordLines > 0 }
    }

    /** `mm:ss.xxx`, millisecond precision (the parser accepts 1–3 fraction digits). */
    private fun lrcTime(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        val minutes = safe / 60_000L
        val seconds = (safe % 60_000L) / 1_000L
        val millis = safe % 1_000L
        return "%02d:%02d.%03d".format(minutes, seconds, millis)
    }

    private fun decodeXmlEntities(raw: String): String {
        if (!raw.contains('&')) return raw
        return Regex("&(#[xX][0-9a-fA-F]+|#\\d+|amp|lt|gt|quot|apos);").replace(raw) { match ->
            val entity = match.groupValues[1]
            when {
                entity.startsWith("#x") || entity.startsWith("#X") ->
                    entity.substring(2).toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: match.value
                entity.startsWith("#") ->
                    entity.substring(1).toIntOrNull()?.let { String(Character.toChars(it)) } ?: match.value
                entity == "amp" -> "&"
                entity == "lt" -> "<"
                entity == "gt" -> ">"
                entity == "quot" -> "\""
                entity == "apos" -> "'"
                else -> match.value
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Decryption: QQ Triple-DES (decrypt) + zlib inflate.
    // ---------------------------------------------------------------------------------------

    fun decrypt(hex: String): String {
        val data = ByteArray(hex.length / 2) { i ->
            ((Character.digit(hex[i * 2], 16) shl 4) or Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
        val schedule = arrayOf(
            keySchedule(QRC_KEY, 16, DECRYPT),
            keySchedule(QRC_KEY, 8, ENCRYPT),
            keySchedule(QRC_KEY, 0, DECRYPT),
        )
        val plain = ByteArray(data.size - data.size % 8)
        var offset = 0
        while (offset + 8 <= data.size) {
            var block = data.copyOfRange(offset, offset + 8)
            for (round in 0 until 3) block = crypt(block, schedule[round])
            block.copyInto(plain, offset)
            offset += 8
        }
        val inflater = Inflater()
        inflater.setInput(plain)
        val out = ByteArrayOutputStream(plain.size * 4)
        val buffer = ByteArray(8192)
        try {
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) break
                }
                out.write(buffer, 0, count)
            }
        } finally {
            inflater.end()
        }
        return out.toString("UTF-8")
    }

    private const val ENCRYPT = 1
    private const val DECRYPT = 0

    // QQ's S-boxes. Note sbox 2 row 2 ("8, 15, 12") and sbox 4 row 4 ("10, 10, 13") differ from
    // standard DES; that is intentional, it is what QQ Music encrypts with.
    private val SBOX = arrayOf(
        intArrayOf(
            14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7,
            0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8,
            4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0,
            15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13,
        ),
        intArrayOf(
            15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10,
            3, 13, 4, 7, 15, 2, 8, 15, 12, 0, 1, 10, 6, 9, 11, 5,
            0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15,
            13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9,
        ),
        intArrayOf(
            10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8,
            13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1,
            13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7,
            1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12,
        ),
        intArrayOf(
            7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15,
            13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9,
            10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4,
            3, 15, 0, 6, 10, 10, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14,
        ),
        intArrayOf(
            2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9,
            14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6,
            4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14,
            11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3,
        ),
        intArrayOf(
            12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11,
            10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8,
            9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6,
            4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13,
        ),
        intArrayOf(
            4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1,
            13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6,
            1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2,
            6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12,
        ),
        intArrayOf(
            13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7,
            1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2,
            7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8,
            2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11,
        ),
    )

    /** Bit [b] of [a] (QQ's little-endian-word bit order, starting at [base]) moved to position [c]. */
    private fun bitNum(a: ByteArray, base: Int, b: Int, c: Int): Int =
        (((a[base + (b / 32) * 4 + 3 - (b % 32) / 8].toInt() and 0xff) ushr (7 - b % 8)) and 1) shl c

    private fun bitNumIntR(a: Int, b: Int, c: Int): Int = ((a ushr (31 - b)) and 1) shl c

    private fun bitNumIntL(a: Int, b: Int, c: Int): Int = ((a shl b) and 0x80000000.toInt()) ushr c

    private fun sboxBit(a: Int): Int = (a and 32) or ((a and 31) ushr 1) or ((a and 1) shl 4)

    private val IP_LEFT = intArrayOf(57, 49, 41, 33, 25, 17, 9, 1, 59, 51, 43, 35, 27, 19, 11, 3, 61, 53, 45, 37, 29, 21, 13, 5, 63, 55, 47, 39, 31, 23, 15, 7)
    private val IP_RIGHT = intArrayOf(56, 48, 40, 32, 24, 16, 8, 0, 58, 50, 42, 34, 26, 18, 10, 2, 60, 52, 44, 36, 28, 20, 12, 4, 62, 54, 46, 38, 30, 22, 14, 6)

    private fun crypt(input: ByteArray, key: Array<IntArray>): ByteArray {
        var s0 = 0
        var s1 = 0
        for (i in 0 until 32) {
            s0 = s0 or bitNum(input, 0, IP_LEFT[i], 31 - i)
            s1 = s1 or bitNum(input, 0, IP_RIGHT[i], 31 - i)
        }
        for (idx in 0 until 15) {
            val previous = s1
            s1 = f(s1, key[idx]) xor s0
            s0 = previous
        }
        s0 = f(s1, key[15]) xor s0
        return inversePermutation(s0, s1)
    }

    private fun inversePermutation(s0: Int, s1: Int): ByteArray {
        val data = ByteArray(8)
        // Output byte order 3,2,1,0,7,6,5,4 takes bit groups 7,6,5,4,3,2,1,0 respectively.
        val byteOrder = intArrayOf(3, 2, 1, 0, 7, 6, 5, 4)
        for (k in 0 until 8) {
            val base = 7 - k
            data[byteOrder[k]] = (
                bitNumIntR(s1, base, 7) or bitNumIntR(s0, base, 6) or
                    bitNumIntR(s1, base + 8, 5) or bitNumIntR(s0, base + 8, 4) or
                    bitNumIntR(s1, base + 16, 3) or bitNumIntR(s0, base + 16, 2) or
                    bitNumIntR(s1, base + 24, 1) or bitNumIntR(s0, base + 24, 0)
                ).toByte()
        }
        return data
    }

    private val P_PERM = intArrayOf(15, 6, 19, 20, 28, 11, 27, 16, 0, 14, 22, 25, 4, 17, 30, 9, 1, 7, 23, 13, 31, 26, 2, 8, 18, 12, 29, 5, 21, 10, 3, 24)

    private fun f(state: Int, key: IntArray): Int {
        val t1 = bitNumIntL(state, 31, 0) or ((state and 0xf0000000.toInt()) ushr 1) or bitNumIntL(state, 4, 5) or
            bitNumIntL(state, 3, 6) or ((state and 0x0f000000) ushr 3) or bitNumIntL(state, 8, 11) or
            bitNumIntL(state, 7, 12) or ((state and 0x00f00000) ushr 5) or bitNumIntL(state, 12, 17) or
            bitNumIntL(state, 11, 18) or ((state and 0x000f0000) ushr 7) or bitNumIntL(state, 16, 23)
        val t2 = bitNumIntL(state, 15, 0) or ((state and 0x0000f000) shl 15) or bitNumIntL(state, 20, 5) or
            bitNumIntL(state, 19, 6) or ((state and 0x00000f00) shl 13) or bitNumIntL(state, 24, 11) or
            bitNumIntL(state, 23, 12) or ((state and 0x000000f0) shl 11) or bitNumIntL(state, 28, 17) or
            bitNumIntL(state, 27, 18) or ((state and 0x0000000f) shl 9) or bitNumIntL(state, 0, 23)
        val l0 = ((t1 ushr 24) and 0xff) xor key[0]
        val l1 = ((t1 ushr 16) and 0xff) xor key[1]
        val l2 = ((t1 ushr 8) and 0xff) xor key[2]
        val l3 = ((t2 ushr 24) and 0xff) xor key[3]
        val l4 = ((t2 ushr 16) and 0xff) xor key[4]
        val l5 = ((t2 ushr 8) and 0xff) xor key[5]
        val s = (SBOX[0][sboxBit(l0 ushr 2)] shl 28) or
            (SBOX[1][sboxBit(((l0 and 0x03) shl 4) or (l1 ushr 4))] shl 24) or
            (SBOX[2][sboxBit(((l1 and 0x0f) shl 2) or (l2 ushr 6))] shl 20) or
            (SBOX[3][sboxBit(l2 and 0x3f)] shl 16) or
            (SBOX[4][sboxBit(l3 ushr 2)] shl 12) or
            (SBOX[5][sboxBit(((l3 and 0x03) shl 4) or (l4 ushr 4))] shl 8) or
            (SBOX[6][sboxBit(((l4 and 0x0f) shl 2) or (l5 ushr 6))] shl 4) or
            SBOX[7][sboxBit(l5 and 0x3f)]
        var result = 0
        for (i in 0 until 32) result = result or bitNumIntL(s, P_PERM[i], i)
        return result
    }

    private val KEY_RND_SHIFT = intArrayOf(1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1)
    private val KEY_PERM_C = intArrayOf(56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17, 9, 1, 58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35)
    private val KEY_PERM_D = intArrayOf(62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21, 13, 5, 60, 52, 44, 36, 28, 20, 12, 4, 27, 19, 11, 3)
    private val KEY_COMPRESSION = intArrayOf(
        13, 16, 10, 23, 0, 4, 2, 27, 14, 5, 20, 9, 22, 18, 11, 3, 25, 7, 15, 6, 26, 19, 12, 1,
        40, 51, 30, 36, 46, 54, 29, 39, 50, 44, 32, 47, 43, 48, 38, 55, 33, 52, 45, 41, 49, 35, 28, 31,
    )

    private fun keySchedule(key: ByteArray, offset: Int, mode: Int): Array<IntArray> {
        val schedule = Array(16) { IntArray(6) }
        var c = 0
        var d = 0
        for (i in 0 until 28) {
            c += bitNum(key, offset, KEY_PERM_C[i], 31 - i)
            d += bitNum(key, offset, KEY_PERM_D[i], 31 - i)
        }
        for (i in 0 until 16) {
            val shift = KEY_RND_SHIFT[i]
            c = ((c shl shift) or (c ushr (28 - shift))) and 0xfffffff0.toInt()
            d = ((d shl shift) or (d ushr (28 - shift))) and 0xfffffff0.toInt()
            val target = if (mode == DECRYPT) 15 - i else i
            val row = schedule[target]
            for (j in 0 until 6) row[j] = 0
            for (j in 0 until 24) row[j / 8] = row[j / 8] or bitNumIntR(c, KEY_COMPRESSION[j], 7 - (j % 8))
            for (j in 24 until 48) row[j / 8] = row[j / 8] or bitNumIntR(d, KEY_COMPRESSION[j] - 27, 7 - (j % 8))
        }
        return schedule
    }
}
