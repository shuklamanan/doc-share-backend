package com.example.doceditor.exception

import com.example.doceditor.dtos.MessageResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    // Handles document/user not found
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(MessageResponse(e.message ?: "Resource not found"))
    }

    // Handles permission checks from your service
    @ExceptionHandler(SecurityException::class)
    fun handleSecurityException(e: SecurityException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(MessageResponse(e.message ?: "Access denied"))
    }

    // Handles general bad requests and file parsing errors
    @ExceptionHandler(IllegalArgumentException::class, RuntimeException::class)
    fun handleBadRequest(e: Exception): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(MessageResponse(e.message ?: "An error occurred"))
    }
}