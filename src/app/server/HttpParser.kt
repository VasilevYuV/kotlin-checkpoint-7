package app.server

import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

object HttpParser {
    fun parse(inputStream: InputStream): HttpRequest? {
        val input = BufferedInputStream(inputStream)

        val requestLine = readLine(input) ?: return null
        if (requestLine.isBlank()) {
            return null
        }
        val parts = requestLine.split(" ")
        if (parts.size < 3) {
            return null
        }

        val method = parts[0]
        val path = parts[1]
        val version = parts[2]

        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = readLine(input) ?: return null
            if (line.isEmpty()) {
                break
            }
            val separatorIndex = line.indexOf(':')
            if (separatorIndex <= 0) {
                continue
            }
            val key = line.substring(0, separatorIndex).trim().lowercase()
            val value = line.substring(separatorIndex + 1).trim()
            headers[key] = value
        }

        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
        val bodyBytes = ByteArray(contentLength)
        var readTotal = 0
        while (readTotal < contentLength) {
            val read = input.read(bodyBytes, readTotal, contentLength - readTotal)
            if (read == -1) {
                break
            }
            readTotal += read
        }

        val body = String(bodyBytes, 0, readTotal, StandardCharsets.UTF_8)
        return HttpRequest(method, path, version, headers, body)
    }

    private fun readLine(input: BufferedInputStream): String? {
        val bytes = mutableListOf<Byte>()
        while (true) {
            val value = input.read()
            if (value == -1) {
                return if (bytes.isEmpty()) null else String(bytes.toByteArray(), StandardCharsets.UTF_8)
            }
            val byte = value.toByte()
            if (byte == '\n'.code.toByte()) {
                break
            }
            if (byte != '\r'.code.toByte()) {
                bytes.add(byte)
            }
        }
        return String(bytes.toByteArray(), StandardCharsets.UTF_8)
    }
}
