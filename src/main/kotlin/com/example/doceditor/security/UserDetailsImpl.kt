package com.example.doceditor.security

import com.example.doceditor.model.User
import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

class UserDetailsImpl(
    val id: UUID,
    private val email: String,
    @JsonIgnore
    private val passwordHash: String,
    val fullName: String
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()

    override fun getPassword(): String = passwordHash

    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true

    companion object {
        fun build(user: User): UserDetailsImpl {
            return UserDetailsImpl(
                id = user.id,
                email = user.email,
                passwordHash = user.passwordHash,
                fullName = user.fullName
            )
        }
    }
}
