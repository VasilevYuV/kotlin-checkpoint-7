package app.repository

import app.data.Session
import app.util.JsonUtil
import java.io.File
import java.time.Instant
import java.util.UUID

class SessionRepository(private val file: File) {
    private val lock = Any()

    init {
        ensureFile()
    }

    fun create(username: String): Session = synchronized(lock) {
        val sessions = readAll().toMutableList()
        val token = UUID.randomUUID().toString().replace("-", "")
        val session = Session(token, username, Instant.now().toString())
        sessions.add(session)
        writeAll(sessions)
        session
    }

    fun findByToken(token: String): Session? = synchronized(lock) {
        readAll().find { it.token == token }
    }

    private fun readAll(): List<Session> {
        ensureFile()
        val raw = file.readText(Charsets.UTF_8)
        return JsonUtil.arrayObjects(raw).mapNotNull { obj ->
            val token = JsonUtil.stringField(obj, "token") ?: return@mapNotNull null
            val username = JsonUtil.stringField(obj, "username") ?: return@mapNotNull null
            val createdAt = JsonUtil.stringField(obj, "createdAt") ?: return@mapNotNull null
            Session(token, username, createdAt)
        }
    }

    private fun writeAll(sessions: List<Session>) {
        val content = sessions.joinToString(
            prefix = "[\n",
            postfix = "\n]",
            separator = ",\n"
        ) { session ->
            """  {"token":"${JsonUtil.escape(session.token)}","username":"${JsonUtil.escape(session.username)}","createdAt":"${JsonUtil.escape(session.createdAt)}"}"""
        }
        file.writeText(content, Charsets.UTF_8)
    }

    private fun ensureFile() {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.writeText("[]", Charsets.UTF_8)
        }
    }
}
