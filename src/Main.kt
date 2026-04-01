import app.controller.RouterController
import app.repository.MessageRepository
import app.repository.SessionRepository
import app.repository.UserRepository
import app.server.MiniHttpServer
import app.service.AuthService
import app.service.MessageService
import app.util.RequestLogger
import java.io.File

fun main() {
    val storageDir = File("storage")
    if (!storageDir.exists()) {
        storageDir.mkdirs()
    }

    val userRepository = UserRepository(File(storageDir, "users.json"))
    val sessionRepository = SessionRepository(File(storageDir, "sessions.json"))
    val messageRepository = MessageRepository(File(storageDir, "messages.json"))

    val authService = AuthService(userRepository, sessionRepository)
    val messageService = MessageService(messageRepository)
    val logger = RequestLogger(File(storageDir, "server.log"))

    val router = RouterController(authService, messageService)
    val server = MiniHttpServer(8080, router, logger)

    println("Server started at http://localhost:8080")
    server.start()
}