package pl.unorank.mobile.auth

import java.time.Instant

class AuthFailure(message: String, val invalidSession: Boolean = false) : Exception(message)
data class Administrator(val id: Long, val username: String)
data class SavedSession(val server: String, val token: String, val expiresAt: Instant)
data class AuthenticatedSession(val server: String, val administrator: Administrator)

interface SessionStore {
    fun load(): SavedSession?
    fun save(session: SavedSession)
    fun clear()
    fun server(): String
    fun saveServer(server: String)
}

interface Authentication {
    suspend fun server(): String
    suspend fun hasSession(): Boolean
    suspend fun restore(): AuthenticatedSession?
    suspend fun login(server: String, username: String, password: String): AuthenticatedSession
    suspend fun logout()
}
