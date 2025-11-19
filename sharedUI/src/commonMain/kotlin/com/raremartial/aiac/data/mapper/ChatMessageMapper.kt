package com.raremartial.aiac.data.mapper

import com.raremartial.aiac.data.model.ChatMessage
import com.raremartial.aiac.data.model.MessageRole
import com.raremartial.aiac.network.models.Message
import com.raremartial.aiac.network.models.ResponseMessage
import com.raremartial.aiac.util.currentTime
import kotlinx.datetime.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
object ChatMessageMapper {
    
    fun toDomain(networkMessage: ResponseMessage, timestamp: kotlin.time.Instant = currentTime()): ChatMessage {
        return ChatMessage(
            id = generateId(),
            content = networkMessage.text,
            role = if (networkMessage.role == "user") MessageRole.USER else MessageRole.ASSISTANT,
            timestamp = timestamp
        )
    }
    
    fun toNetwork(domainMessage: ChatMessage): Message {
        return Message(
            role = if (domainMessage.role == MessageRole.USER) "user" else "assistant",
            text = domainMessage.content
        )
    }
    
    fun generateId(): String {
        return "msg_${currentTime().epochSeconds}_${kotlin.random.Random.nextInt(1000, 9999)}"
    }
}

