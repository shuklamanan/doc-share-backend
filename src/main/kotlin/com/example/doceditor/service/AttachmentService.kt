package com.example.doceditor.service

import com.example.doceditor.model.Attachment
import com.example.doceditor.model.Document
import com.example.doceditor.repository.AttachmentRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@Service
class AttachmentService(
    private val attachmentRepository: AttachmentRepository
) {
    private val logger = LoggerFactory.getLogger(AttachmentService::class.java)

    @Value("\${app.upload.dir}")
    private lateinit var uploadDir: String

    fun saveAttachment(document: Document, file: MultipartFile): Attachment {
        val originalFilename = file.originalFilename ?: "unnamed"
        logger.info("[Attachment] Upload requested for file '$originalFilename' under document '${document.title}' (${document.id})")

        val rootPath = Paths.get(uploadDir).toAbsolutePath().normalize()
        val uploadFolder = File(rootPath.toString())
        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs()
        }

        val storageFilename = "${UUID.randomUUID()}-$originalFilename"
        val targetLocation = rootPath.resolve(storageFilename)

        Files.copy(file.inputStream, targetLocation)

        val attachment = Attachment(
            document = document,
            fileName = originalFilename,
            fileSize = file.size,
            contentType = file.contentType ?: "application/octet-stream",
            filePath = targetLocation.toString()
        )

        val savedAttachment = attachmentRepository.save(attachment)
        logger.info("[Attachment] File '$originalFilename' saved successfully as '${storageFilename}' with attachment ID: ${savedAttachment.id}")
        return savedAttachment
    }

    fun getAttachmentsForDocument(document: Document): List<Attachment> {
        logger.info("[Attachment] Retrieving attachments for document '${document.title}' (${document.id})")
        return attachmentRepository.findByDocumentOrderByUploadedAtDesc(document)
    }

    fun getAttachmentById(id: UUID): Attachment? {
        logger.info("[Attachment] Retrieving attachment file details for ID: $id")
        return attachmentRepository.findById(id).orElse(null)
    }
}
