package com.example.doceditor.service

import com.example.doceditor.model.Attachment
import com.example.doceditor.model.Document
import com.example.doceditor.repository.AttachmentRepository
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
    @Value("\${app.upload.dir}")
    private lateinit var uploadDir: String

    fun saveAttachment(document: Document, file: MultipartFile): Attachment {
        val rootPath = Paths.get(uploadDir).toAbsolutePath().normalize()
        val uploadFolder = File(rootPath.toString())
        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs()
        }

        val originalFilename = file.originalFilename ?: "unnamed"
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

        return attachmentRepository.save(attachment)
    }

    fun getAttachmentsForDocument(document: Document): List<Attachment> {
        return attachmentRepository.findByDocumentOrderByUploadedAtDesc(document)
    }

    fun getAttachmentById(id: UUID): Attachment? {
        return attachmentRepository.findById(id).orElse(null)
    }
}
