package app.service

import app.data.Message
import app.repository.MessageRepository

class MessageService(private val messageRepository: MessageRepository) {
    fun create(author: String, text: String): Message? {
        if (text.isBlank()) {
            return null
        }
        return messageRepository.create(author = author, text = text.trim())
    }

    fun getAll(): List<Message> {
        return messageRepository.getAll()
    }

    fun delete(messageId: Int, currentUser: String): DeleteResult {
        val target = messageRepository.findById(messageId) ?: return DeleteResult.NOT_FOUND
        if (target.author != currentUser) {
            return DeleteResult.FORBIDDEN
        }
        messageRepository.deleteById(messageId)
        return DeleteResult.DELETED
    }
}

enum class DeleteResult {
    DELETED,
    FORBIDDEN,
    NOT_FOUND
}
