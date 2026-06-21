package com.example.doceditor.repository

import com.example.doceditor.model.Attachment
import com.example.doceditor.model.Document
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AttachmentRepository : JpaRepository<Attachment, UUID> {
    fun findByDocumentOrderByUploadedAtDesc(document: Document): List<Attachment>
}
