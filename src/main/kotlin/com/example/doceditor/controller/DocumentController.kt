package com.example.doceditor.controller

import com.example.doceditor.dtos.*
import com.example.doceditor.model.Document
import com.example.doceditor.security.UserDetailsImpl
import com.example.doceditor.service.AttachmentService
import com.example.doceditor.service.DocumentService
import jakarta.validation.Valid
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.UUID

@RestController
@RequestMapping("/api/documents")
class DocumentController(
    private val documentService: DocumentService,
    private val attachmentService: AttachmentService
) {

    private fun mapToResponse(doc: Document): DocumentResponse {
        return DocumentResponse(
            id = doc.id,
            title = doc.title,
            content = doc.content,
            ownerId = doc.owner.id,
            ownerEmail = doc.owner.email,
            ownerName = doc.owner.fullName,
            createdAt = doc.createdAt,
            updatedAt = doc.updatedAt
        )
    }

    @PostMapping
    fun createDocument(
        @Valid @RequestBody request: CreateDocumentRequest,
        @AuthenticationPrincipal userDetails: UserDetailsImpl
    ): ResponseEntity<DocumentResponse> {
        val doc = documentService.createDocument(request.title, userDetails.id)
        return ResponseEntity.ok(mapToResponse(doc))
    }

    @GetMapping("/owned")
    fun getOwnedDocuments(@AuthenticationPrincipal userDetails: UserDetailsImpl): ResponseEntity<List<DocumentResponse>> {
        val docs = documentService.getOwnedDocuments(userDetails.id).map { mapToResponse(it) }
        return ResponseEntity.ok(docs)
    }

    @GetMapping("/shared")
    fun getSharedDocuments(@AuthenticationPrincipal userDetails: UserDetailsImpl): ResponseEntity<List<DocumentResponse>> {
        val docs = documentService.getSharedDocuments(userDetails.id).map { mapToResponse(it) }
        return ResponseEntity.ok(docs)
    }

    @GetMapping("/{id}")
    fun getDocumentById(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userDetails: UserDetailsImpl
    ): ResponseEntity<DocumentResponse> {
        val doc = documentService.getDocumentByIdAndCheckAccess(id, userDetails.id)
        return ResponseEntity.ok(mapToResponse(doc))
    }

    @PutMapping("/{id}/rename")
    fun renameDocument(
        @PathVariable id: UUID,
        @Valid @RequestBody request: RenameDocumentRequest,
        @AuthenticationPrincipal userDetails: UserDetailsImpl
    ): ResponseEntity<DocumentResponse> {
        val doc = documentService.renameDocument(id, request.title, userDetails.id)
        return ResponseEntity.ok(mapToResponse(doc))
    }

    @PutMapping("/{id}/content")
    fun saveDocumentContent(
        @PathVariable id: UUID,
        @RequestBody request: SaveContentRequest,
        @AuthenticationPrincipal userDetails: UserDetailsImpl
    ): ResponseEntity<DocumentResponse> {
        val doc = documentService.saveDocumentContent(id, request.content, userDetails.id)
        return ResponseEntity.ok(mapToResponse(doc))
    }

    @DeleteMapping("/{id}")
    fun deleteDocument(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userDetails: UserDetailsImpl
    ): ResponseEntity<MessageResponse> {
        documentService.deleteDocument(id, userDetails.id)
        return ResponseEntity.ok(MessageResponse("Document deleted successfully"))
    }

    @PostMapping("/{id}/share")
    fun shareDocument(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ShareRequest,
        @AuthenticationPrincipal userDetails: UserDetailsImpl
    ): ResponseEntity<MessageResponse> {
        documentService.shareDocument(id, request.email, userDetails.id)
        return ResponseEntity.ok(MessageResponse("Document shared successfully with ${request.email}"))
    }

    @PostMapping("/import")
    fun importDocument(
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal userDetails: UserDetailsImpl
    ): ResponseEntity<DocumentResponse> {
        val doc = documentService.importDocumentFromFile(file, userDetails.id)
        return ResponseEntity.ok(mapToResponse(doc))
    }

    @PostMapping("/{id}/attachments")
    fun uploadAttachment(
        @PathVariable id: UUID,
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal userDetails: UserDetailsImpl
    ): ResponseEntity<AttachmentResponse> {
        val doc = documentService.getDocumentByIdAndCheckAccess(id, userDetails.id)
        val attachment = attachmentService.saveAttachment(doc, file)

        return ResponseEntity.ok(
            AttachmentResponse(
                id = attachment.id,
                fileName = attachment.fileName,
                fileSize = attachment.fileSize,
                contentType = attachment.contentType,
                uploadedAt = attachment.uploadedAt
            )
        )
    }

    @GetMapping("/{id}/attachments")
    fun getAttachments(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userDetails: UserDetailsImpl
    ): ResponseEntity<List<AttachmentResponse>> {
        val doc = documentService.getDocumentByIdAndCheckAccess(id, userDetails.id)
        val attachments = attachmentService.getAttachmentsForDocument(doc).map {
            AttachmentResponse(
                id = it.id,
                fileName = it.fileName,
                fileSize = it.fileSize,
                contentType = it.contentType,
                uploadedAt = it.uploadedAt
            )
        }
        return ResponseEntity.ok(attachments)
    }

    @GetMapping("/attachments/{attachmentId}")
    fun downloadAttachment(
        @PathVariable attachmentId: UUID,
        @AuthenticationPrincipal userDetails: UserDetailsImpl
    ): ResponseEntity<FileSystemResource> {
        val attachment = attachmentService.getAttachmentById(attachmentId)
            ?: throw NoSuchElementException("Attachment not found")

        documentService.validateAccess(attachment.document, userDetails.id)

        val file = File(attachment.filePath)
        if (!file.exists()) {
            throw RuntimeException("File not found on server")
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${attachment.fileName}\"")
            .contentType(MediaType.parseMediaType(attachment.contentType))
            .contentLength(attachment.fileSize)
            .body(FileSystemResource(file))
    }
}