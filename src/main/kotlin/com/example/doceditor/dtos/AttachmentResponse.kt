package com.example.doceditor.dtos

import java.time.Instant
import java.util.UUID

data class AttachmentResponse(
    val id: UUID,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val uploadedAt: Instant
)