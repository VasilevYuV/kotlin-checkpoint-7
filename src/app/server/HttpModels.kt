package app.server

import java.nio.charset.StandardCharsets

data class HttpRequest(
    val method: String,
    val path: String,
    val version: String,
    val headers: Map<String, String>,
    val body: String
)

data class HttpResponse(
    val statusCode: Int,
    val statusText: String,
    val body: String,
    val headers: MutableMap<String, String> = mutableMapOf("Content-Type" to "application/json; charset=utf-8")
) {
    fun toBytes(): ByteArray {
        val bodyBytes = body.toByteArray(StandardCharsets.UTF_8)
        headers["Content-Length"] = bodyBytes.size.toString()
        headers["Connection"] = "close"

        val builder = StringBuilder()
        builder.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n")
        headers.forEach { (key, value) ->
            builder.append(key).append(": ").append(value).append("\r\n")
        }
        builder.append("\r\n")

        return builder.toString().toByteArray(StandardCharsets.UTF_8) + bodyBytes
    }
}
