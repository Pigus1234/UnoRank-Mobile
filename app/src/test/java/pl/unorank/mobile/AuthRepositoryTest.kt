package pl.unorank.mobile

import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import pl.unorank.mobile.auth.*

class MemorySessionStore : SessionStore {
    var session: SavedSession? = null
    var address = ""
    override fun load() = session
    override fun save(session: SavedSession) { this.session = session }
    override fun clear() { session = null }
    override fun server() = address
    override fun saveServer(server: String) { address = server }
}

class AuthRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var store: MemorySessionStore
    private lateinit var repository: AuthRepository
    private val future = Instant.now().plusSeconds(3600).toString()

    @Before fun setup() {
        server = MockWebServer().apply { start() }
        store = MemorySessionStore()
        repository = AuthRepository(AuthApi(), store, true, "Test phone")
    }

    @After fun close() { server.shutdown() }

    private fun response(code: Int = 200, body: String = "{}") = MockResponse().setResponseCode(code).setBody(body).addHeader("Content-Type", "application/json")
    private fun loginResponse() = response(body = """{"token":"42|secret-token","expires_at":"$future"}""")
    private fun meResponse() = response(body = """{"data":{"id":1,"username":"owner"}}""")
    private fun savedSession() = SavedSession(ServerAddress.parse(server.url("/").toString(), true).value, "42|secret-token", Instant.parse(future))

    @Test fun loginUsesBackendContractAndStoresTokenOnly() = runBlocking {
        server.enqueue(loginResponse())
        server.enqueue(meResponse())
        val result = repository.login(server.url("/").toString(), "owner", "Password123")
        assertEquals("owner", result.administrator.username)
        val login = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("/api/v1/auth/login", login.path)
        assertEquals("POST", login.method)
        assertNull(login.getHeader("Authorization"))
        val json = org.json.JSONObject(login.body.readUtf8())
        assertEquals("Password123", json.getString("password"))
        assertEquals("Test phone", json.getString("device_name"))
        val me = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("/api/v1/auth/me", me.path)
        assertEquals("Bearer 42|secret-token", me.getHeader("Authorization"))
        assertEquals("42|secret-token", store.session!!.token)
        assertFalse(store.session.toString().contains("Password123"))
    }

    @Test fun invalidPasswordDoesNotCreateSavedSession() = runBlocking {
        server.enqueue(response(401))
        val error = runCatching { repository.login(server.url("/").toString(), "owner", "wrong") }.exceptionOrNull() as AuthFailure
        assertEquals("Nieprawidłowy login lub hasło.", error.message)
        assertFalse(error.invalidSession)
        assertNull(store.session)
    }

    @Test fun missingAdminAccessClearsIssuedToken() = runBlocking {
        server.enqueue(loginResponse())
        server.enqueue(response(403))
        val error = runCatching { repository.login(server.url("/").toString(), "owner", "password") }.exceptionOrNull() as AuthFailure
        assertTrue(error.invalidSession)
        assertNull(store.session)
    }

    @Test fun transientVerificationFailureKeepsSessionForRetry() = runBlocking {
        server.enqueue(loginResponse())
        server.enqueue(response(503))
        assertTrue(runCatching { repository.login(server.url("/").toString(), "owner", "password") }.isFailure)
        assertNotNull(store.session)
        server.enqueue(meResponse())
        assertEquals("owner", repository.restore()!!.administrator.username)
        assertEquals(3, server.requestCount)
    }

    @Test fun unauthorizedRestoreClearsSession() = runBlocking {
        store.session = savedSession()
        server.enqueue(response(401))
        val error = runCatching { repository.restore() }.exceptionOrNull() as AuthFailure
        assertTrue(error.invalidSession)
        assertNull(store.session)
    }

    @Test fun expiredTokenIsClearedWithoutSendingRequest() = runBlocking {
        store.session = savedSession().copy(expiresAt = Instant.now().minusSeconds(1))
        assertTrue(runCatching { repository.restore() }.isFailure)
        assertNull(store.session)
        assertEquals(0, server.requestCount)
    }

    @Test fun logoutRevokesCurrentTokenThenClearsLocalSession() = runBlocking {
        store.session = savedSession()
        server.enqueue(response(204, ""))
        repository.logout()
        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("/api/v1/auth/logout", request.path)
        assertEquals("POST", request.method)
        assertEquals("Bearer 42|secret-token", request.getHeader("Authorization"))
        assertNull(store.session)
    }

    @Test fun failedLogoutRetainsSessionAndExpiredLogoutClearsIt() = runBlocking {
        store.session = savedSession()
        server.enqueue(response(503))
        assertTrue(runCatching { repository.logout() }.isFailure)
        assertNotNull(store.session)
        server.enqueue(response(401))
        repository.logout()
        assertNull(store.session)
    }

    @Test fun redirectCannotForwardCredentialsOrToken() = runBlocking {
        store.session = savedSession()
        server.enqueue(response(307).addHeader("Location", server.url("/unexpected")))
        assertTrue(runCatching { repository.restore() }.isFailure)
        assertEquals(1, server.requestCount)
        assertNotNull(store.session)
    }

    @Test fun malformedAndRateLimitedResponsesBecomeSafeMessages() = runBlocking {
        for (reply in listOf(response(body = "<html>failure</html>"), response(body = """{"token":"secret"}"""), response(429))) {
            server.enqueue(reply)
            val failure = runCatching { repository.login(server.url("/").toString(), "owner", "password") }.exceptionOrNull()
            assertTrue(failure is AuthFailure)
            assertFalse(failure!!.message!!.contains("secret"))
            assertNull(store.session)
        }
    }

    @Test fun serverAddressNormalizesSupportedPathsAndBlocksInsecureRemoteAddresses() {
        assertEquals("https://example.com/api/v1/", ServerAddress.parse(" https://example.com/api/v1 ", false).value)
        assertEquals("https://example.com/api/v1/", ServerAddress.parse("https://example.com", false).value)
        for (url in listOf("http://example.com", "http://127.0.0.1:8000", "https://owner:password@example.com", "https://example.com?token=secret", "https://example.com/wrong", "https://example.com#part")) {
            assertTrue(url, runCatching { ServerAddress.parse(url, false) }.isFailure)
        }
        assertEquals("http://10.0.2.2:8000/api/v1/", ServerAddress.parse("http://10.0.2.2:8000", true).value)
        assertTrue(runCatching { ServerAddress.parse("http://192.168.1.10:8000", true) }.isFailure)
    }
}
