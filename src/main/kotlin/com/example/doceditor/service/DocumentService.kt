package com.example.doceditor.service

import com.example.doceditor.model.Document
import com.example.doceditor.model.DocumentPermission
import com.example.doceditor.model.User
import com.example.doceditor.repository.DocumentPermissionRepository
import com.example.doceditor.repository.DocumentRepository
import com.example.doceditor.repository.UserRepository
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.UUID

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val documentPermissionRepository: DocumentPermissionRepository,
    private val userRepository: UserRepository
) {

    // Internal helper to lookup user
    private fun getUser(userId: UUID): User {
        return userRepository.findById(userId).orElseThrow { NoSuchElementException("User not found") }
    }

    fun createDocument(title: String, ownerId: UUID, content: String? = null): Document {
        val owner = getUser(ownerId)
        val doc = Document(
            title = title,
            content = content ?: "<p></p>",
            owner = owner
        )
        return documentRepository.save(doc)
    }

    fun getOwnedDocuments(ownerId: UUID): List<Document> {
        val owner = getUser(ownerId)
        return documentRepository.findByOwnerOrderByUpdatedAtDesc(owner)
    }

    fun getSharedDocuments(userId: UUID): List<Document> {
        val user = getUser(userId)
        return documentRepository.findSharedWithUserOrderByUpdatedAtDesc(user)
    }

    // New wrapper method to enforce security checks
    fun getDocumentByIdAndCheckAccess(documentId: UUID, userId: UUID): Document {
        val document = documentRepository.findById(documentId).orElseThrow { NoSuchElementException("Document not found") }
        validateAccess(document, userId)
        return document
    }

    fun validateAccess(document: Document, userId: UUID) {
        val user = getUser(userId)
        if (document.owner.id != user.id && !documentPermissionRepository.existsByDocumentAndUser(document, user)) {
            throw SecurityException("Access denied")
        }
    }

    @Transactional
    fun renameDocument(documentId: UUID, newTitle: String, userId: UUID): Document {
        val document = getDocumentByIdAndCheckAccess(documentId, userId)
        document.title = newTitle
        document.updatedAt = Instant.now()
        return documentRepository.save(document)
    }

    @Transactional
    fun saveDocumentContent(documentId: UUID, content: String, userId: UUID): Document {
        val document = getDocumentByIdAndCheckAccess(documentId, userId)
        document.content = content
        document.updatedAt = Instant.now()
        return documentRepository.save(document)
    }

    @Transactional
    fun deleteDocument(documentId: UUID, userId: UUID) {
        val document = documentRepository.findById(documentId).orElseThrow { NoSuchElementException("Document not found") }
        if (document.owner.id != userId) {
            throw SecurityException("Only the owner can delete a document")
        }
        documentRepository.delete(document)
    }

    @Transactional
    fun shareDocument(documentId: UUID, inviteeEmail: String, ownerId: UUID): DocumentPermission {
        val document = documentRepository.findById(documentId).orElseThrow { NoSuchElementException("Document not found") }

        if (document.owner.id != ownerId) {
            throw SecurityException("Only the owner can share a document")
        }

        val invitee = userRepository.findByEmail(inviteeEmail)
            ?: throw NoSuchElementException("User with email $inviteeEmail not found")

        if (invitee.id == ownerId) {
            throw IllegalArgumentException("You cannot share a document with yourself")
        }

        val existing = documentPermissionRepository.findByDocumentAndUser(document, invitee)
        if (existing != null) return existing

        return documentPermissionRepository.save(DocumentPermission(document = document, user = invitee))
    }

    @Transactional
    fun importDocumentFromFile(file: MultipartFile, ownerId: UUID): Document {
        val filename = file.originalFilename ?: "Imported Document"
        val title = filename.substringBeforeLast(".")
        val extension = filename.substringAfterLast(".").lowercase()

        val parsedContent = when (extension) {
            "txt", "md" -> String(file.bytes, Charsets.UTF_8)
            "docx" -> parseDocx(file)
            else -> throw IllegalArgumentException("Unsupported file format: .$extension")
        }

        val formattedContent = if (parsedContent.trim().isEmpty()) {
            "<p></p>"
        } else {
            parsedContent.split("\n")
                .filter { it.trim().isNotEmpty() }
                .joinToString("") { "<p>${it.replace("<", "&lt;").replace(">", "&gt;")}</p>" }
        }

        return createDocument(title, ownerId, formattedContent)
    }

    private fun parseDocx(file: MultipartFile): String {
        XWPFDocument(file.inputStream).use { doc ->
            return doc.paragraphs.joinToString("\n") { it.text }
        }
    }
}