package com.example.doceditor.dtos

import com.example.doceditor.enums.CoeditMessageType
import java.util.UUID

data class CoeditMessage(
    val type: CoeditMessageType,
    val documentId: UUID,
    val senderId: UUID,
    val senderName: String,
    val content: String? = null,
    val cursorIndex: Int? = null,
    val cursorLength: Int? = null
)