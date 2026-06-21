package com.example.doceditor.config

import com.example.doceditor.security.JwtUtils
import com.example.doceditor.security.UserDetailsServiceImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val jwtUtils: JwtUtils,
    private val userDetailsService: UserDetailsServiceImpl,
    @Value("\${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private val allowedOriginsStr: String
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic")
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        val origins = allowedOriginsStr.split(",").map { it.trim() }.toMutableList()
        if (!origins.contains("http://localhost:*")) origins.add("http://localhost:*")
        if (!origins.contains("http://127.0.0.1:*")) origins.add("http://127.0.0.1:*")

        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(*origins.toTypedArray())
            .withSockJS()
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(object : ChannelInterceptor {
            override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
                val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
                if (accessor != null && StompCommand.CONNECT == accessor.command) {
                    val token = accessor.getFirstNativeHeader("token")
                        ?: accessor.getFirstNativeHeader("Authorization")?.replace("Bearer ", "")
                    if (!token.isNullOrEmpty() && jwtUtils.validateJwtToken(token)) {
                        val username = jwtUtils.getUsernameFromJwtToken(token)
                        val userDetails = userDetailsService.loadUserByUsername(username)
                        val authentication = UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.authorities
                        )
                        accessor.user = authentication
                    }
                }
                return message
            }
        })
    }
}
