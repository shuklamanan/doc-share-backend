package com.example.doceditor.dtos

data class SignupRequest(
    val email: String,
    val password: String,
    val fullName: String
)