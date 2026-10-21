package pl.unorank.mobile

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import pl.unorank.mobile.auth.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val loggedIn = AuthenticatedSession("https://example.com/api/v1/", Administrator(1, "owner"))

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    private inner class FakeAuthentication : Authentication {
        var saved = false
        var failure: Exception? = null
        var gate: CompletableDeferred<Unit>? = null
        var loginCalls = 0
        override suspend fun server() = "https://example.com/api/v1/"
        override suspend fun hasSession() = saved
        override suspend fun restore(): AuthenticatedSession? {
            failure?.let { throw it }
            return if (saved) loggedIn else null
        }
        override suspend fun login(server: String, username: String, password: String): AuthenticatedSession {
            loginCalls++
            gate?.await()
            failure?.let { throw it }
            saved = true
            return loggedIn
        }
        override suspend fun logout() {
            failure?.let { throw it }
            saved = false
        }
    }

    @Test fun startupWithoutTokenShowsLoginAndSuccessfulLoginShowsMenu() = runTest(dispatcher) {
        val repository = FakeAuthentication()
        val model = AuthViewModel(repository)
        runCurrent()
        assertEquals(AuthScreen.Login, model.state.value.screen)
        model.login("https://example.com", "owner", "password")
        runCurrent()
        assertEquals(AuthScreen.Home, model.state.value.screen)
        assertEquals("owner", model.state.value.administrator)
        assertFalse(model.state.value.busy)
        model.logout()
        runCurrent()
        assertEquals(AuthScreen.Login, model.state.value.screen)
        assertEquals("", model.state.value.administrator)
    }

    @Test fun duplicateTapDoesNotCreateTwoLogins() = runTest(dispatcher) {
        val repository = FakeAuthentication()
        val model = AuthViewModel(repository)
        runCurrent()
        repository.gate = CompletableDeferred()
        model.login("https://example.com", "owner", "password")
        model.login("https://example.com", "owner", "password")
        runCurrent()
        assertEquals(1, repository.loginCalls)
        assertTrue(model.state.value.busy)
        repository.gate!!.complete(Unit)
        runCurrent()
        assertEquals(AuthScreen.Home, model.state.value.screen)
    }

    @Test fun savedSessionNetworkFailureOffersRecoveryWithoutShowingHome() = runTest(dispatcher) {
        val repository = FakeAuthentication().apply { saved = true; failure = AuthFailure("Brak połączenia") }
        val model = AuthViewModel(repository)
        runCurrent()
        assertEquals(AuthScreen.Recovery, model.state.value.screen)
        assertEquals("Brak połączenia", model.state.value.error)
        repository.failure = null
        model.restore()
        runCurrent()
        assertEquals(AuthScreen.Home, model.state.value.screen)
    }

    @Test fun failedLogoutDoesNotPretendTheAccountWasLoggedOut() = runTest(dispatcher) {
        val repository = FakeAuthentication().apply { saved = true }
        val model = AuthViewModel(repository)
        runCurrent()
        repository.failure = AuthFailure("Brak połączenia")
        model.logout()
        runCurrent()
        assertEquals(AuthScreen.Home, model.state.value.screen)
        assertNotNull(model.state.value.error)
        assertTrue(repository.saved)
    }

    @Test fun unexpectedErrorNeverExposesSensitiveExceptionText() = runTest(dispatcher) {
        val repository = FakeAuthentication().apply { failure = IllegalStateException("secret-token") }
        val model = AuthViewModel(repository)
        runCurrent()
        assertEquals(AuthScreen.Login, model.state.value.screen)
        assertFalse(model.state.value.error!!.contains("secret-token"))
    }
}
