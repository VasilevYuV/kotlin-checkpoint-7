package app.repository

import app.data.User
import app.util.JsonUtil
import java.io.File

class UserRepository(private val file: File) {
    private val lock = Any()

    init {
        ensureFile()
    }

    fun register(user: User): Boolean = synchronized(lock) {
        val users = readAll().toMutableList()
        if (users.any { it.username == user.username }) {
            return@synchronized false
        }
        users.add(user)
        writeAll(users)
        true
    }

    fun validateCredentials(username: String, password: String): Boolean = synchronized(lock) {
        readAll().any { it.username == username && it.password == password }
    }

    fun findByUsername(username: String): User? = synchronized(lock) {
        readAll().find { it.username == username }
    }

    private fun readAll(): List<User> {
        ensureFile()
        val raw = file.readText(Charsets.UTF_8)
        return JsonUtil.arrayObjects(raw).mapNotNull { obj ->
            val username = JsonUtil.stringField(obj, "username") ?: return@mapNotNull null
            val password = JsonUtil.stringField(obj, "password") ?: return@mapNotNull null
            User(username, password)
        }
    }

    private fun writeAll(users: List<User>) {
        val content = users.joinToString(
            prefix = "[\n",
            postfix = "\n]",
            separator = ",\n"
        ) { user ->
            """  {"username":"${JsonUtil.escape(user.username)}","password":"${JsonUtil.escape(user.password)}"}"""
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
