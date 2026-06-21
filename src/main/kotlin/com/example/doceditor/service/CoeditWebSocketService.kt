package com.example.doceditor.service

import com.example.doceditor.dtos.CoeditMessage
import com.example.doceditor.enums.CoeditMessageType
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class CollaboratorSession(
    val sessionId: String,
    val userId: UUID,
    val userName: String
)

@Service
class CoeditWebSocketService (
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = LoggerFactory.getLogger(CoeditWebSocketService::class.java)
    private val activeUsers = ConcurrentHashMap<UUID, ConcurrentHashMap<String, CollaboratorSession>>()

    fun handleCoeditMessage (
        documentId: UUID,
        message: CoeditMessage,
        headerAccessor: SimpMessageHeaderAccessor
    ) {
        val userMap = activeUsers.computeIfAbsent(documentId) { ConcurrentHashMap() }
        val sessionId = headerAccessor.sessionId ?: return

        when (message.type) {
            CoeditMessageType.JOIN -> {
                logger.info("[WebSocket] User '${message.senderName}' (${message.senderId}) JOINED session for document $documentId (Session ID: $sessionId)")
                userMap[sessionId] = CollaboratorSession(
                    sessionId = sessionId,
                    userId = message.senderId,
                    userName = message.senderName
                )
                headerAccessor.sessionAttributes?.put("userId", message.senderId.toString())
                headerAccessor.sessionAttributes?.put("documentId", documentId.toString())
                headerAccessor.sessionAttributes?.put("senderName", message.senderName)
                broadcastActiveUsers(documentId)
            }
            CoeditMessageType.LEAVE -> {
                logger.info("[WebSocket] User '${message.senderName}' (${message.senderId}) LEFT session for document $documentId (Session ID: $sessionId)")
                userMap.remove(sessionId)
                broadcastActiveUsers(documentId)
            }
            CoeditMessageType.EDIT -> {
                logger.info("[WebSocket] User '${message.senderName}' (${message.senderId}) EDITED content of document $documentId")
                messagingTemplate.convertAndSend("/topic/documents/$documentId", message)
            }
            CoeditMessageType.CURSOR -> {
                logger.info("[WebSocket] User '${message.senderName}' (${message.senderId}) updated cursor position in document $documentId")
                messagingTemplate.convertAndSend("/topic/documents/$documentId", message)
            }
        }
    }

    private fun broadcastActiveUsers(documentId: UUID) {
        val sessions = activeUsers[documentId]?.values ?: emptyList()
        // Group sessions by userId to get a list of unique active users
        val uniqueUsers = sessions.groupBy { it.userId }.map { (userId, userSessions) ->
            mapOf("id" to userId.toString(), "name" to userSessions.first().userName)
        }

        messagingTemplate.convertAndSend("/topic/documents/$documentId/presence", uniqueUsers)
    }

    @EventListener
    fun handleSessionDisconnect(event: SessionDisconnectEvent) {
        val sessionId = event.sessionId
        val headerAccessor = SimpMessageHeaderAccessor.wrap(event.message)
        val docIdStr = headerAccessor.sessionAttributes?.get("documentId") as? String
        val senderName = headerAccessor.sessionAttributes?.get("senderName") as? String ?: "Unknown"
        val userIdStr = headerAccessor.sessionAttributes?.get("userId") as? String ?: "Unknown"

        if (docIdStr != null) {
            val documentId = UUID.fromString(docIdStr)
            logger.info("[WebSocket] Session disconnected: '$senderName' ($userIdStr) left document $documentId (Session ID: $sessionId)")
            val userMap = activeUsers[documentId]
            if (userMap != null) {
                userMap.remove(sessionId)
                if (userMap.isEmpty()) {
                    activeUsers.remove(documentId)
                } else {
                    broadcastActiveUsers(documentId)
                }
            }
        }
    }
}