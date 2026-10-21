package pl.unorank.mobile

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.unorank.mobile.auth.AuthViewModel
import pl.unorank.mobile.ui.UnoRankApp
import pl.unorank.mobile.ui.UnoRankTheme

class MainActivity : ComponentActivity() {
    private val model: AuthViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(AuthViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel((application as UnoRankApplication).authentication) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            UnoRankTheme {
                UnoRankApp(
                    state = model.state.collectAsStateWithLifecycle().value,
                    onLogin = model::login,
                    onRetry = model::restore,
                    onLogout = model::logout,
                    onExit = { finish() },
                )
            }
        }
    }
}
