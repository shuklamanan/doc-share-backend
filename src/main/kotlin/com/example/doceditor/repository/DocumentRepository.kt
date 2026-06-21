package com.example.doceditor.repository

import com.example.doceditor.model.Document
import com.example.doceditor.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface DocumentRepository : JpaRepository<Document, UUID> {
    fun findByOwnerOrderByUpdatedAtDesc(owner: User): List<Document>

    @Query("SELECT d FROM Document d JOIN DocumentPermission dp ON d.id = dp.document.id WHERE dp.user = :user ORDER BY d.updatedAt DESC")
    fun findSharedWithUserOrderByUpdatedAtDesc(user: User): List<Document>
}
