package com.example.doceditor.controller

import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
class HealthController {

    @GetMapping("/api/health")
    fun healthCheck(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "message" to "Application is running and healthy"
        )
    }
}
