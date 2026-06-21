package com.example.doceditor.repository

import com.example.doceditor.model.RefreshToken
import com.example.doceditor.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    fun findByToken(token: String): RefreshToken?

    @Modifying
    fun deleteByUser(user: User): Int
}
