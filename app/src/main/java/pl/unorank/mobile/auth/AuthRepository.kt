package pl.unorank.mobile.auth

import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val api: AuthApi,
    private val store: SessionStore,
    private val allowLocalHttp: Boolean,
    private val deviceName: String,
    private val clock: Clock = Clock.systemUTC(),
) : Authentication {
    override suspend fun server(): String = withContext(Dispatchers.IO) { store.server() }
    override suspend fun hasSession(): Boolean = withContext(Dispatchers.IO) { store.load() != null }

    override suspend fun restore(): AuthenticatedSession? = withContext(Dispatchers.IO) {
        val saved = store.load() ?: return@withContext null
        if (!saved.expiresAt.isAfter(Instant.now(clock))) {
            store.clear()
            throw AuthFailure("Sesja wygasła. Zaloguj się ponownie.", true)
        }
        verify(saved)
    }

    override suspend fun login(server: String, username: String, password: String): AuthenticatedSession = withContext(Dispatchers.IO) {
        val address = ServerAddress.parse(server, allowLocalHttp)
        if (!username.matches(Regex("[a-z0-9._-]{1,60}")) || password.isEmpty() || password.length > 255) {
            throw AuthFailure("Uzupełnij login i hasło administratora.")
        }
        check(store.load() == null)
        store.saveServer(address.value)
        val saved = api.login(address, username, password, deviceName.take(100))
        store.save(saved)
        verify(saved)
    }

    override suspend fun logout(): Unit = withContext(Dispatchers.IO) {
        val saved = store.load() ?: return@withContext
        try {
            api.logout(ServerAddress.parse(saved.server, allowLocalHttp), saved.token)
        } catch (failure: AuthFailure) {
            if (!failure.invalidSession) throw failure
        }
        store.clear()
    }

    private suspend fun verify(saved: SavedSession): AuthenticatedSession {
        return try {
            AuthenticatedSession(saved.server, api.me(ServerAddress.parse(saved.server, allowLocalHttp), saved.token))
        } catch (failure: AuthFailure) {
            if (failure.invalidSession) store.clear()
            throw failure
        }
    }
}
