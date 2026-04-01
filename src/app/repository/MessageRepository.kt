package app.repository

import app.data.Message
import app.util.JsonUtil
import java.io.File
import java.time.Instant

class MessageRepository(private val file: File) {
    private val lock = Any()

    init {
        ensureFile()
    }

    fun create(author: String, text: String): Message = synchronized(lock) {
        val messages = readAll().toMutableList()
        val newId = (messages.maxOfOrNull { it.id } ?: 0) + 1
        val message = Message(
            id = newId,
            author = author,
            text = text,
            createdAt = Instant.now().toString()
        )
        messages.add(message)
        writeAll(messages)
        message
    }

    fun getAll(): List<Message> = synchronized(lock) {
        readAll()
    }

    fun findById(id: Int): Message? = synchronized(lock) {
        readAll().find { it.id == id }
    }

    fun deleteById(id: Int): Message? = synchronized(lock) {
        val messages = readAll().toMutableList()
        val target = messages.find { it.id == id } ?: return@synchronized null
        messages.removeIf { it.id == id }
        writeAll(messages)
        target
    }

    private fun readAll(): List<Message> {
        ensureFile()
        val raw = file.readText(Charsets.UTF_8)
        return JsonUtil.arrayObjects(raw).mapNotNull { obj ->
            val id = JsonUtil.intField(obj, "id") ?: return@mapNotNull null
            val author = JsonUtil.stringField(obj, "author") ?: return@mapNotNull null
            val text = JsonUtil.stringField(obj, "text") ?: return@mapNotNull null
            val createdAt = JsonUtil.stringField(obj, "createdAt") ?: return@mapNotNull null
            Message(id, author, text, createdAt)
        }.sortedBy { it.id }
    }

    private fun writeAll(messages: List<Message>) {
        val content = messages.joinToString(
            prefix = "[\n",
            postfix = "\n]",
            separator = ",\n"
        ) { message ->
            """  {"id":${message.id},"author":"${JsonUtil.escape(message.author)}","text":"${JsonUtil.escape(message.text)}","createdAt":"${JsonUtil.escape(message.createdAt)}"}"""
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
