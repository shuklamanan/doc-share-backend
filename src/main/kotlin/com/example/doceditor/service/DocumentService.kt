package com.example.doceditor.service

import com.example.doceditor.model.Document
import com.example.doceditor.model.DocumentPermission
import com.example.doceditor.model.User
import com.example.doceditor.repository.DocumentPermissionRepository
import com.example.doceditor.repository.DocumentRepository
import com.example.doceditor.repository.UserRepository
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(DocumentService::class.java)

    // Internal helper to lookup user
    private fun getUser(userId: UUID): User {
        return userRepository.findById(userId).orElseThrow { NoSuchElementException("User not found") }
    }

    fun createDocument(title: String, ownerId: UUID, content: String? = null): Document {
        val owner = getUser(ownerId)
        logger.info("[Doc] User ${owner.fullName} (${owner.email}) is creating a new document titled '$title'")
        val doc = Document(
            title = title,
            content = content ?: "<p></p>",
            owner = owner
        )
        val savedDoc = documentRepository.save(doc)
        logger.info("[Doc] Document created successfully with ID: ${savedDoc.id} for owner ${owner.fullName}")
        return savedDoc
    }

    fun getOwnedDocuments(ownerId: UUID): List<Document> {
        val owner = getUser(ownerId)
        logger.info("[Doc] Fetching owned documents for user ${owner.fullName} (${owner.email})")
        return documentRepository.findByOwnerOrderByUpdatedAtDesc(owner)
    }

    fun getSharedDocuments(userId: UUID): List<Document> {
        val user = getUser(userId)
        logger.info("[Doc] Fetching shared documents for user ${user.fullName} (${user.email})")
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
            logger.warn("[Doc] Security warning: Access denied for user ${user.fullName} to document '${document.title}' (${document.id})")
            throw SecurityException("Access denied")
        }
    }

    @Transactional
    fun renameDocument(documentId: UUID, newTitle: String, userId: UUID): Document {
        val user = getUser(userId)
        val document = getDocumentByIdAndCheckAccess(documentId, userId)
        logger.info("[Doc] User ${user.fullName} (${user.email}) is renaming document '${document.title}' (${document.id}) to '$newTitle'")
        document.title = newTitle
        document.updatedAt = Instant.now()
        val updatedDoc = documentRepository.save(document)
        logger.info("[Doc] Document renamed successfully to '$newTitle'")
        return updatedDoc
    }

    @Transactional
    fun saveDocumentContent(documentId: UUID, content: String, userId: UUID): Document {
        val user = getUser(userId)
        val document = getDocumentByIdAndCheckAccess(documentId, userId)
        logger.info("[Doc] User ${user.fullName} (${user.email}) is saving updated content for document '${document.title}' (${document.id})")
        document.content = content
        document.updatedAt = Instant.now()
        val updatedDoc = documentRepository.save(document)
        logger.info("[Doc] Document content saved successfully")
        return updatedDoc
    }

    @Transactional
    fun deleteDocument(documentId: UUID, userId: UUID) {
        val user = getUser(userId)
        val document = documentRepository.findById(documentId).orElseThrow { NoSuchElementException("Document not found") }
        if (document.owner.id != userId) {
            logger.warn("[Doc] User ${user.fullName} attempted to delete document '${document.title}' (${document.id}) but is not the owner!")
            throw SecurityException("Only the owner can delete a document")
        }
        logger.info("[Doc] Owner ${user.fullName} (${user.email}) is deleting document '${document.title}' (${document.id})")
        documentRepository.delete(document)
        logger.info("[Doc] Document deleted successfully")
    }

    @Transactional
    fun shareDocument(documentId: UUID, inviteeEmail: String, ownerId: UUID): DocumentPermission {
        val owner = getUser(ownerId)
        val document = documentRepository.findById(documentId).orElseThrow { NoSuchElementException("Document not found") }

        if (document.owner.id != ownerId) {
            logger.warn("[Doc] User ${owner.fullName} attempted to share document '${document.title}' (${document.id}) but is not the owner!")
            throw SecurityException("Only the owner can share a document")
        }

        logger.info("[Doc] Owner ${owner.fullName} (${owner.email}) is sharing document '${document.title}' (${document.id}) with invitee '$inviteeEmail'")

        val invitee = userRepository.findByEmail(inviteeEmail)
            ?: throw NoSuchElementException("User with email $inviteeEmail not found")

        if (invitee.id == ownerId) {
            logger.warn("[Doc] Share failed: Owner attempted to share document with themselves")
            throw java.lang.IllegalArgumentException("You cannot share a document with yourself")
        }

        val existing = documentPermissionRepository.findByDocumentAndUser(document, invitee)
        if (existing != null) {
            logger.info("[Doc] Invite skipped: Document '${document.title}' is already shared with user '${invitee.fullName}' (${invitee.email})")
            return existing
        }

        val permission = documentPermissionRepository.save(DocumentPermission(document = document, user = invitee))
        logger.info("[Doc] Document '${document.title}' shared successfully with '${invitee.fullName}' (${invitee.email})")
        return permission
    }

    @Transactional
    fun importDocumentFromFile(file: MultipartFile, ownerId: UUID): Document {
        val owner = getUser(ownerId)
        val filename = file.originalFilename ?: "Imported Document"
        val title = filename.substringBeforeLast(".")
        val extension = filename.substringAfterLast(".").lowercase()

        logger.info("[Doc] User ${owner.fullName} (${owner.email}) is importing document from file '$filename'")

        val parsedContent = when (extension) {
            "txt", "md" -> String(file.bytes, Charsets.UTF_8)
            "docx" -> parseDocx(file)
            else -> throw java.lang.IllegalArgumentException("Unsupported file format: .$extension")
        }

        val formattedContent = if (parsedContent.trim().isEmpty()) {
            "<p></p>"
        } else {
            parsedContent.split("\n")
                .filter { it.trim().isNotEmpty() }
                .joinToString("") { "<p>${it.replace("<", "&lt;").replace(">", "&gt;")}</p>" }
        }

        val doc = createDocument(title, ownerId, formattedContent)
        logger.info("[Doc] Document imported successfully from '$filename' with ID: ${doc.id}")
        return doc
    }

    private fun parseDocx(file: MultipartFile): String {
        XWPFDocument(file.inputStream).use { doc ->
            return doc.paragraphs.joinToString("\n") { it.text }
        }
    }
}