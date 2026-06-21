package com.example.doceditor.repository

import com.example.doceditor.model.Document
import com.example.doceditor.model.DocumentPermission
import com.example.doceditor.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DocumentPermissionRepository : JpaRepository<DocumentPermission, UUID> {
    fun findByDocumentAndUser(document: Document, user: User): DocumentPermission?
    fun findByDocument(document: Document): List<DocumentPermission>
    fun existsByDocumentAndUser(document: Document, user: User): Boolean
    fun deleteByDocumentAndUser(document: Document, user: User)
}
