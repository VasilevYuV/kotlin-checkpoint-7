package app.controller

import app.server.HttpRequest
import app.server.HttpResponse
import app.service.AuthService
import app.service.DeleteResult
import app.service.MessageService
import app.util.JsonUtil

data class RouteResult(
    val response: HttpResponse,
    val userForLog: String?
)

class RouterController(
    private val authService: AuthService,
    private val messageService: MessageService
) {
    fun handle(request: HttpRequest): RouteResult {
        return try {
            when {
                request.method == "POST" && request.path == "/register" -> handleRegister(request)
                request.method == "POST" && request.path == "/login" -> handleLogin(request)
                request.method == "POST" && request.path == "/messages" -> handleCreateMessage(request)
                request.method == "GET" && request.path == "/messages" -> handleGetMessages()
                request.method == "DELETE" && request.path.matches(Regex("^/messages/\\d+$")) -> handleDeleteMessage(request)
                else -> RouteResult(
                    HttpResponse(404, "Not Found", """{"error":"Route not found"}"""),
                    userForLog = "guest"
                )
            }
        } catch (_: Exception) {
            RouteResult(
                HttpResponse(500, "Internal Server Error", """{"error":"Internal server error"}"""),
                userForLog = "guest"
            )
        }
    }

    private fun handleRegister(request: HttpRequest): RouteResult {
        val username = JsonUtil.stringField(request.body, "username")
        val password = JsonUtil.stringField(request.body, "password")
        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            return RouteResult(
                HttpResponse(400, "Bad Request", """{"error":"username and password are required"}"""),
                userForLog = "guest"
            )
        }
        val registered = authService.register(username, password)
        return if (registered) {
            RouteResult(
                HttpResponse(201, "Created", """{"message":"User registered"}"""),
                userForLog = username
            )
        } else {
            RouteResult(
                HttpResponse(400, "Bad Request", """{"error":"User already exists or invalid data"}"""),
                userForLog = username
            )
        }
    }

    private fun handleLogin(request: HttpRequest): RouteResult {
        val username = JsonUtil.stringField(request.body, "username")
        val password = JsonUtil.stringField(request.body, "password")
        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            return RouteResult(
                HttpResponse(400, "Bad Request", """{"error":"username and password are required"}"""),
                userForLog = "guest"
            )
        }
        val token = authService.login(username, password)
            ?: return RouteResult(
                HttpResponse(401, "Unauthorized", """{"error":"Invalid credentials"}"""),
                userForLog = username
            )
        return RouteResult(
            HttpResponse(200, "OK", """{"token":"${JsonUtil.escape(token)}"}"""),
            userForLog = username
        )
    }

    private fun handleCreateMessage(request: HttpRequest): RouteResult {
        val username = requireUser(request) ?: return RouteResult(
            HttpResponse(401, "Unauthorized", """{"error":"Unauthorized"}"""),
            userForLog = "guest"
        )
        val text = JsonUtil.stringField(request.body, "text")
        if (text.isNullOrBlank()) {
            return RouteResult(
                HttpResponse(400, "Bad Request", """{"error":"text is required"}"""),
                userForLog = username
            )
        }
        val message = messageService.create(username, text) ?: return RouteResult(
            HttpResponse(400, "Bad Request", """{"error":"text is required"}"""),
            userForLog = username
        )
        val body = """{"id":${message.id},"author":"${JsonUtil.escape(message.author)}","text":"${JsonUtil.escape(message.text)}","createdAt":"${JsonUtil.escape(message.createdAt)}"}"""
        return RouteResult(HttpResponse(201, "Created", body), userForLog = username)
    }

    private fun handleGetMessages(): RouteResult {
        val messages = messageService.getAll()
        val body = messages.joinToString(prefix = "[", postfix = "]", separator = ",") { m ->
            """{"id":${m.id},"author":"${JsonUtil.escape(m.author)}","text":"${JsonUtil.escape(m.text)}","createdAt":"${JsonUtil.escape(m.createdAt)}"}"""
        }
        return RouteResult(HttpResponse(200, "OK", body), userForLog = "guest")
    }

    private fun handleDeleteMessage(request: HttpRequest): RouteResult {
        val username = requireUser(request) ?: return RouteResult(
            HttpResponse(401, "Unauthorized", """{"error":"Unauthorized"}"""),
            userForLog = "guest"
        )
        val id = request.path.substringAfterLast("/").toIntOrNull()
            ?: return RouteResult(
                HttpResponse(400, "Bad Request", """{"error":"Invalid message id"}"""),
                userForLog = username
            )
        return when (messageService.delete(id, username)) {
            DeleteResult.DELETED -> RouteResult(
                HttpResponse(200, "OK", """{"message":"Message deleted"}"""),
                userForLog = username
            )

            DeleteResult.FORBIDDEN -> RouteResult(
                HttpResponse(403, "Forbidden", """{"error":"Only author can delete message"}"""),
                userForLog = username
            )

            DeleteResult.NOT_FOUND -> RouteResult(
                HttpResponse(404, "Not Found", """{"error":"Message not found"}"""),
                userForLog = username
            )
        }
    }

    private fun requireUser(request: HttpRequest): String? {
        val authHeader = request.headers["authorization"] ?: return null
        if (!authHeader.startsWith("Bearer ")) {
            return null
        }
        val token = authHeader.removePrefix("Bearer ").trim()
        return authService.authenticate(token)
    }
}
