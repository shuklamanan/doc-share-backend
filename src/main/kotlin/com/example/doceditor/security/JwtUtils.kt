package com.example.doceditor.security

import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key
import java.util.Date

@Component
class JwtUtils {
    private val logger = LoggerFactory.getLogger(JwtUtils::class.java)

    @Value("\${app.jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${app.jwt.accessTokenExpirationMs}")
    private var jwtExpirationMs: Long = 900000

    @Value("\${app.jwt.refreshTokenExpirationMs}")
    private var refreshTokenExpirationMs: Long = 604800000

    private fun getSigningKey(): javax.crypto.SecretKey {
        val keyBytes = Decoders.BASE64.decode(jwtSecret)
        return Keys.hmacShaKeyFor(keyBytes)
    }

    fun generateJwtToken(userDetails: UserDetailsImpl): String {
        return Jwts.builder()
            .subject(userDetails.username)
            .claim("id", userDetails.id.toString())
            .claim("fullName", userDetails.fullName)
            .issuedAt(Date())
            .expiration(Date(Date().time + jwtExpirationMs))
            .signWith(getSigningKey())
            .compact()
    }

    fun generateJwtTokenFromUsername(username: String): String {
        return Jwts.builder()
            .subject(username)
            .issuedAt(Date())
            .expiration(Date(Date().time + jwtExpirationMs))
            .signWith(getSigningKey())
            .compact()
    }

    fun generateRefreshToken(username: String): String {
        return Jwts.builder()
            .subject(username)
            .claim("type", "refresh")
            .issuedAt(Date())
            .expiration(Date(Date().time + refreshTokenExpirationMs))
            .signWith(getSigningKey())
            .compact()
    }

    fun getRefreshTokenExpirationSeconds(): Long {
        return refreshTokenExpirationMs / 1000
    }

    fun getUsernameFromJwtToken(token: String): String {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
    }

    fun validateJwtToken(authToken: String): Boolean {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(authToken)
            return true
        } catch (e: MalformedJwtException) {
            logger.error("Invalid JWT token: ${e.message}")
        } catch (e: ExpiredJwtException) {
            logger.error("JWT token is expired: ${e.message}")
        } catch (e: UnsupportedJwtException) {
            logger.error("JWT token is unsupported: ${e.message}")
        } catch (e: IllegalArgumentException) {
            logger.error("JWT claims string is empty: ${e.message}")
        }
        return false
    }
}
