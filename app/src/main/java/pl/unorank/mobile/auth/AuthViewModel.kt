package pl.unorank.mobile.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthScreen { Checking, Login, Recovery, Home }
data class AuthUiState(
    val screen: AuthScreen = AuthScreen.Checking,
    val server: String = "",
    val administrator: String = "",
    val busy: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(private val repository: Authentication) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state = mutableState.asStateFlow()

    init { restore() }

    fun restore() = perform {
        val server = repository.server()
        mutableState.value = mutableState.value.copy(server = server, error = null)
        val session = repository.restore()
        mutableState.value = session?.toUiState() ?: AuthUiState(AuthScreen.Login, server)
    }

    fun login(server: String, username: String, password: String) = perform {
        mutableState.value = mutableState.value.copy(server = server)
        mutableState.value = repository.login(server, username.trim(), password).toUiState()
    }

    fun logout() = perform {
        repository.logout()
        mutableState.value = AuthUiState(AuthScreen.Login, repository.server())
    }

    private fun perform(operation: suspend () -> Unit) {
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                val saved = runCatching { repository.hasSession() }.getOrDefault(false)
                val screen = if (saved) {
                    if (mutableState.value.screen == AuthScreen.Home) AuthScreen.Home else AuthScreen.Recovery
                } else AuthScreen.Login
                mutableState.value = mutableState.value.copy(screen = screen, error = (failure as? AuthFailure)?.message ?: "Nie udało się ukończyć operacji. Spróbuj ponownie.")
            } finally {
                mutableState.value = mutableState.value.copy(busy = false)
            }
        }
    }

    private fun AuthenticatedSession.toUiState() = AuthUiState(AuthScreen.Home, server, administrator.username)
}
