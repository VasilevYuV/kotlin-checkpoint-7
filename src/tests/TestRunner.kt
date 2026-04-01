package tests

import app.repository.MessageRepository
import app.repository.SessionRepository
import app.repository.UserRepository
import app.service.AuthService
import app.service.DeleteResult
import app.service.MessageService
import java.io.File
import java.nio.file.Files

fun main() {
    val tests = listOf(
        ::testUserRegisterAndDuplicate,
        ::testLoginAndTokenAuth,
        ::testMessageCreateListDeleteFlow,
        ::testDeleteForbiddenAndNotFound
    )

    var passed = 0
    tests.forEach { test ->
        try {
            test()
            println("[PASS] ${test.name}")
            passed++
        } catch (e: Exception) {
            println("[FAIL] ${test.name}: ${e.message}")
        }
    }

    println("Result: $passed/${tests.size} tests passed")
    if (passed != tests.size) {
        throw IllegalStateException("Some tests failed")
    }
}

private fun testUserRegisterAndDuplicate() {
    withTestContext { authService, _ ->
        val first = authService.register("alex", "12345")
        val duplicate = authService.register("alex", "12345")
        val invalid = authService.register("", "12345")

        assertTrue(first, "First register must succeed")
        assertTrue(!duplicate, "Duplicate user must be rejected")
        assertTrue(!invalid, "Blank username must be rejected")
    }
}

private fun testLoginAndTokenAuth() {
    withTestContext { authService, _ ->
        authService.register("alex", "12345")
        val token = authService.login("alex", "12345")
        val badToken = authService.login("alex", "wrong")

        assertTrue(!token.isNullOrBlank(), "Valid login must return token")
        assertTrue(badToken == null, "Invalid login must fail")
        assertTrue(authService.authenticate(token) == "alex", "Token must resolve to user")
    }
}

private fun testMessageCreateListDeleteFlow() {
    withTestContext { authService, messageService ->
        authService.register("alex", "12345")
        val created = messageService.create("alex", "Hello world")
        val all = messageService.getAll()

        assertTrue(created != null, "Message must be created")
        assertTrue(all.size == 1, "There must be exactly one message")
        assertTrue(all.first().author == "alex", "Author must match")
        assertTrue(all.first().text == "Hello world", "Text must match")

        val deleteResult = messageService.delete(all.first().id, "alex")
        assertTrue(deleteResult == DeleteResult.DELETED, "Author must be able to delete own message")
        assertTrue(messageService.getAll().isEmpty(), "List must be empty after delete")
    }
}

private fun testDeleteForbiddenAndNotFound() {
    withTestContext { authService, messageService ->
        authService.register("alex", "12345")
        authService.register("maria", "12345")

        val created = messageService.create("alex", "Private message")
            ?: throw IllegalStateException("Message create failed")

        val forbidden = messageService.delete(created.id, "maria")
        val notFound = messageService.delete(9999, "alex")

        assertTrue(forbidden == DeleteResult.FORBIDDEN, "Other user must not delete another users message")
        assertTrue(notFound == DeleteResult.NOT_FOUND, "Unknown id must return NOT_FOUND")
    }
}

private fun withTestContext(testBlock: (AuthService, MessageService) -> Unit) {
    val tempDir = Files.createTempDirectory("mini-http-tests").toFile()
    try {
        val userRepo = UserRepository(File(tempDir, "users.json"))
        val sessionRepo = SessionRepository(File(tempDir, "sessions.json"))
        val messageRepo = MessageRepository(File(tempDir, "messages.json"))

        val authService = AuthService(userRepo, sessionRepo)
        val messageService = MessageService(messageRepo)

        testBlock(authService, messageService)
    } finally {
        tempDir.deleteRecursively()
    }
}

private fun assertTrue(condition: Boolean, message: String) {
    if (!condition) {
        throw IllegalStateException(message)
    }
}
