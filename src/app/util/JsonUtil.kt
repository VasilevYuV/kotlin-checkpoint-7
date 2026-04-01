package app.util

object JsonUtil {
    fun escape(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    fun unescape(value: String): String {
        return value
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    fun stringField(json: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"")
        val match = regex.find(json) ?: return null
        return unescape(match.groupValues[1])
    }

    fun intField(json: String, key: String): Int? {
        val regex = Regex("\"$key\"\\s*:\\s*(\\d+)")
        val match = regex.find(json) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    fun arrayObjects(json: String): List<String> {
        val trimmed = json.trim()
        if (trimmed.isBlank() || trimmed == "[]") {
            return emptyList()
        }
        val body = trimmed.removePrefix("[").removeSuffix("]")
        val result = mutableListOf<String>()

        var depth = 0
        var inString = false
        var escapeNext = false
        var start = -1

        for (i in body.indices) {
            val c = body[i]
            if (escapeNext) {
                escapeNext = false
                continue
            }
            if (c == '\\') {
                escapeNext = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (inString) {
                continue
            }
            if (c == '{') {
                if (depth == 0) {
                    start = i
                }
                depth++
            } else if (c == '}') {
                depth--
                if (depth == 0 && start >= 0) {
                    result.add(body.substring(start, i + 1))
                }
            }
        }

        return result
    }
}
