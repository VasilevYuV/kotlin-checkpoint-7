package app.util

import java.io.File
import java.time.Instant

class RequestLogger(private val file: File) {
    private val lock = Any()

    init {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
    }

    fun log(method: String, path: String, user: String?) {
        val safeUser = user ?: "guest"
        val line = "${Instant.now()} $method $path $safeUser\n"
        synchronized(lock) {
            file.appendText(line, Charsets.UTF_8)
        }
    }
}
