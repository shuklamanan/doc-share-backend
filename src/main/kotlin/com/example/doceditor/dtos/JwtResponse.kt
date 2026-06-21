package com.example.doceditor.dtos

import java.util.UUID

data class JwtResponse(
    val accessToken: String,
    val id: UUID,
    val email: String,
    val fullName: String
)