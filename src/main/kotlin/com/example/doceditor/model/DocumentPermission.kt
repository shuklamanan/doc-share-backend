package com.example.doceditor.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "document_permissions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["document_id", "user_id"])]
)
class DocumentPermission(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    val document: Document,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User
)
