package com.example.doceditor.controller

import com.example.doceditor.dtos.LoginRequest
import com.example.doceditor.dtos.SignupRequest
import com.example.doceditor.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/signup")
    fun registerUser(
        @Valid @RequestBody signupRequest: SignupRequest
    ) = authService.userRegistration(signupRequest)

    @PostMapping("/login")
    fun authenticateUser(
        @Valid @RequestBody loginRequest: LoginRequest,
        response: HttpServletResponse
    ) = authService.login(loginRequest, response)

    @PostMapping("/refresh")
    fun refreshAccessToken(
        request: HttpServletRequest,
        response: HttpServletResponse
    ) = authService.refreshAccessToken(request, response)

    @PostMapping("/logout")
    fun logoutUser(
        response: HttpServletResponse
    ) = authService.logout(response)
}
