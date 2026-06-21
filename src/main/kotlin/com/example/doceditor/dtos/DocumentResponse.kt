package com.example.doceditor.dtos

import java.time.Instant
import java.util.UUID

data class DocumentResponse(
    val id: UUID,
    val title: String,
    val content: String?,
    val ownerId: UUID,
    val ownerEmail: String,
    val ownerName: String,
    val createdAt: Instant,
    val updatedAt: Instant
)