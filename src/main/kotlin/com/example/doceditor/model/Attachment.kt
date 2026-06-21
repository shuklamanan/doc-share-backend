package com.example.doceditor.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "attachments")
class Attachment(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    val document: Document,

    @Column(nullable = false)
    val fileName: String,

    @Column(nullable = false)
    val fileSize: Long,

    @Column(nullable = false)
    val contentType: String,

    @Column(nullable = false)
    val filePath: String,

    @Column(nullable = false)
    val uploadedAt: Instant = Instant.now()
)
