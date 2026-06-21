package com.example.doceditor.service

import com.example.doceditor.model.RefreshToken
import com.example.doceditor.repository.RefreshTokenRepository
import com.example.doceditor.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository
) {
    @Value("\${app.jwt.refreshTokenExpirationMs}")
    private var refreshTokenDurationMs: Long = 604800000

    fun findByToken(token: String): RefreshToken? {
        return refreshTokenRepository.findByToken(token)
    }

    @Transactional
    fun createRefreshToken(userId: UUID): RefreshToken {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        
        refreshTokenRepository.deleteByUser(user)

        val refreshToken = RefreshToken(
            token = UUID.randomUUID().toString(),
            user = user,
            expiryDate = Instant.now().plusMillis(refreshTokenDurationMs)
        )

        return refreshTokenRepository.save(refreshToken)
    }

    fun verifyExpiration(token: RefreshToken): RefreshToken {
        if (token.expiryDate.isBefore(Instant.now())) {
            refreshTokenRepository.delete(token)
            throw RuntimeException("Refresh token was expired. Please login again.")
        }
        return token
    }

    @Transactional
    fun deleteByUserId(userId: UUID): Int {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        return refreshTokenRepository.deleteByUser(user)
    }
}
