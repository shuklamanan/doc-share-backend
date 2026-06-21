package com.example.doceditor.controller

import com.example.doceditor.dtos.CoeditMessage
import com.example.doceditor.enums.CoeditMessageType
import com.example.doceditor.service.CoeditWebSocketService
import org.springframework.context.event.EventListener
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Controller
class CoeditWebSocketController(
    private val coeditWebSocketService: CoeditWebSocketService
) {

    @MessageMapping("/coedit/{documentId}")
    fun handleCoeditMessage(
        @DestinationVariable documentId: UUID,
        @Payload message: CoeditMessage,
        headerAccessor: SimpMessageHeaderAccessor
    ) = coeditWebSocketService.handleCoeditMessage(documentId, message, headerAccessor)
}
