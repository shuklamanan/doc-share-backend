package com.example.doceditor.service

import com.example.doceditor.dtos.CoeditMessage
import com.example.doceditor.enums.CoeditMessageType
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@Service
class CoeditWebSocketService (
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val activeUsers = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, String>>()
    fun handleCoeditMessage (
        documentId: UUID,
        message: CoeditMessage,
        headerAccessor: SimpMessageHeaderAccessor
    ) {
        val userMap = activeUsers.computeIfAbsent(documentId) { ConcurrentHashMap() }

        when (message.type) {
            CoeditMessageType.JOIN -> {
                userMap[message.senderId] = message.senderName
                headerAccessor.sessionAttributes?.put("userId", message.senderId.toString())
                headerAccessor.sessionAttributes?.put("documentId", documentId.toString())
                headerAccessor.sessionAttributes?.put("senderName", message.senderName)
                broadcastActiveUsers(documentId)
            }
            CoeditMessageType.LEAVE -> {
                userMap.remove(message.senderId)
                broadcastActiveUsers(documentId)
            }
            CoeditMessageType.EDIT -> {
                messagingTemplate.convertAndSend("/topic/documents/$documentId", message)
            }
            CoeditMessageType.CURSOR -> {
                messagingTemplate.convertAndSend("/topic/documents/$documentId", message)
            }
        }
    }

    private fun broadcastActiveUsers(documentId: UUID) {
        val users = activeUsers[documentId]?.map { (id, name) ->
            mapOf("id" to id.toString(), "name" to name)
        } ?: emptyList()

        messagingTemplate.convertAndSend("/topic/documents/$documentId/presence", users)
    }

    @EventListener
    fun handleSessionDisconnect(event: SessionDisconnectEvent) {
        val headerAccessor = SimpMessageHeaderAccessor.wrap(event.message)
        val userIdStr = headerAccessor.sessionAttributes?.get("userId") as? String
        val docIdStr = headerAccessor.sessionAttributes?.get("documentId") as? String

        if (userIdStr != null && docIdStr != null) {
            val userId = UUID.fromString(userIdStr)
            val documentId = UUID.fromString(docIdStr)

            val userMap = activeUsers[documentId]
            if (userMap != null) {
                userMap.remove(userId)
                if (userMap.isEmpty()) {
                    activeUsers.remove(documentId)
                } else {
                    broadcastActiveUsers(documentId)
                }
            }
        }
    }
}