package com.example.doceditor.service

import com.example.doceditor.model.User
import com.example.doceditor.dtos.JwtResponse
import com.example.doceditor.dtos.LoginRequest
import com.example.doceditor.dtos.MessageResponse
import com.example.doceditor.dtos.SignupRequest
import com.example.doceditor.dtos.TokenRefreshResponse
import com.example.doceditor.repository.UserRepository
import com.example.doceditor.security.JwtUtils
import com.example.doceditor.security.UserDetailsImpl
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtils: JwtUtils,
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    val refreshTokenCookieKey = "refreshToken"

    fun userRegistration (
        req: SignupRequest,
    ): ResponseEntity<MessageResponse> {
        logger.info("[Auth] Registration request received for email: ${req.email}, name: ${req.fullName}")
        if (userRepository.existsByEmail(req.email)) {
            logger.warn("[Auth] Registration failed: Email ${req.email} is already in use!")
            return ResponseEntity
                .badRequest()
                .body(MessageResponse("Error: Email is already in use!"))
        }

        val user = User(
            email = req.email,
            passwordHash = passwordEncoder.encode(req.password) ?: "",
            fullName = req.fullName
        )

        userRepository.save(user)
        logger.info("[Auth] User registered successfully with email: ${req.email}, name: ${req.fullName}")
        return ResponseEntity.ok(MessageResponse("User registered successfully!"))
    }

    fun login (
        loginRequest: LoginRequest,
        response: HttpServletResponse
    ): ResponseEntity<JwtResponse> {
        logger.info("[Auth] Login request received for email: ${loginRequest.email}")
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(loginRequest.email, loginRequest.password)
        )

        SecurityContextHolder.getContext().authentication = authentication
        val userDetails = authentication.principal as UserDetailsImpl
        val accessToken = jwtUtils.generateJwtToken(userDetails)
        val refreshToken = jwtUtils.generateRefreshToken(userDetails.username)

        // Set refresh token as HttpOnly secure cookie
        addRefreshTokenCookie(response, refreshToken)

        logger.info("[Auth] User logged in successfully: id: ${userDetails.id}, email: ${userDetails.username}, name: ${userDetails.fullName}")
        return ResponseEntity.ok(
            JwtResponse(
                accessToken = accessToken,
                id = userDetails.id,
                email = userDetails.username,
                fullName = userDetails.fullName
            )
        )
    }

    private fun addRefreshTokenCookie (
        response: HttpServletResponse,
        token: String
    ) {
        val cookie = Cookie(refreshTokenCookieKey, token)
        cookie.isHttpOnly = true
        cookie.secure = false // set to true in production with HTTPS
        cookie.path = "/api/auth"
        cookie.maxAge = jwtUtils.getRefreshTokenExpirationSeconds().toInt()
        cookie.setAttribute("SameSite", "Lax")
        response.addCookie(cookie)
    }

    fun refreshAccessToken (
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<*> {
        // Read refresh token from HttpOnly cookie
        val refreshToken = request.cookies
            ?.firstOrNull { it.name == refreshTokenCookieKey }
            ?.value

        if (refreshToken.isNullOrBlank()) {
            logger.warn("[Auth] Token refresh failed: No refresh token cookie found")
            return ResponseEntity.status(401)
                .body(MessageResponse("Error: No refresh token cookie found"))
        }

        // Validate the refresh token JWT
        if (!jwtUtils.validateJwtToken(refreshToken)) {
            logger.warn("[Auth] Token refresh failed: Refresh token is invalid or expired")
            // Clear the invalid cookie
            clearRefreshTokenCookie(response)
            return ResponseEntity.status(401)
                .body(MessageResponse("Error: Refresh token is invalid or expired"))
        }

        val username = jwtUtils.getUsernameFromJwtToken(refreshToken)
        val newAccessToken = jwtUtils.generateJwtTokenFromUsername(username)
        val newRefreshToken = jwtUtils.generateRefreshToken(username)

        // Rotate refresh token cookie
        addRefreshTokenCookie(response, newRefreshToken)

        logger.info("[Auth] Access token successfully refreshed for user: $username")
        return ResponseEntity.ok(TokenRefreshResponse(accessToken = newAccessToken))
    }

    fun logout (
        response: HttpServletResponse
    ): ResponseEntity<MessageResponse> {
        clearRefreshTokenCookie(response)
        logger.info("[Auth] User logged out successfully and refresh token cookie cleared")
        return ResponseEntity.ok(MessageResponse("Log out successful!"))
    }

    private fun clearRefreshTokenCookie(response: HttpServletResponse) {
        val cookie = Cookie(refreshTokenCookieKey, "")
        cookie.isHttpOnly = true
        cookie.secure = false
        cookie.path = "/api/auth"
        cookie.maxAge = 0
        cookie.setAttribute("SameSite", "Lax")
        response.addCookie(cookie)
    }
}