package app.service

import app.data.User
import app.repository.SessionRepository
import app.repository.UserRepository

class AuthService(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) {
    fun register(username: String, password: String): Boolean {
        if (username.isBlank() || password.isBlank()) {
            return false
        }
        return userRepository.register(User(username.trim(), password))
    }

    fun login(username: String, password: String): String? {
        if (!userRepository.validateCredentials(username, password)) {
            return null
        }
        return sessionRepository.create(username).token
    }

    fun authenticate(token: String?): String? {
        if (token.isNullOrBlank()) {
            return null
        }
        return sessionRepository.findByToken(token)?.username
    }
}
