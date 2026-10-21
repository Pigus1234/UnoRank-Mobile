package pl.unorank.mobile

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pl.unorank.mobile.auth.AuthScreen
import pl.unorank.mobile.auth.AuthUiState
import pl.unorank.mobile.ui.UnoRankApp
import pl.unorank.mobile.ui.UnoRankTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w390dp-h844dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UnoRankScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun loginRequiresBothFieldsAndClearsPasswordAfterSubmission() {
        var submitted: List<String>? = null
        compose.setContent {
            UnoRankTheme(true) {
                UnoRankApp(AuthUiState(AuthScreen.Login, "http://127.0.0.1:8000/api/v1/"), { server, user, password -> submitted = listOf(server, user, password) }, {}, {}, {})
            }
        }
        val submit = compose.onNode(hasText("Zaloguj się") and hasClickAction())
        submit.assertIsNotEnabled()
        screenshot("login-dark")
        compose.onNodeWithText("Login").performTextInput("owner")
        compose.onNodeWithText("Hasło").performTextInput("Password123")
        submit.assertIsEnabled().performClick()
        assertEquals("owner", submitted!![1])
        assertEquals("Password123", submitted!![2])
        submit.assertIsNotEnabled()
    }

    @Test fun menuShowsVerifiedAccountAndClearlyMarksUnimplementedFunctions() {
        var exited = false
        compose.setContent {
            UnoRankTheme(true) {
                UnoRankApp(AuthUiState(AuthScreen.Home, administrator = "owner"), { _, _, _ -> }, {}, {}, { exited = true })
            }
        }
        compose.onNodeWithText("Konto potwierdzone").assertIsDisplayed()
        compose.onAllNodesWithText("Wkrótce").assertCountEquals(3)
        compose.onNodeWithText("Rozpocznij grę").assertHasNoClickAction()
        screenshot("home-dark")
        compose.onNodeWithText("Wyjście").performScrollTo().performClick()
        assertTrue(exited)
    }

    @Test fun recoveryAllowsRetryAndShowsTheNetworkError() {
        var retried = false
        compose.setContent {
            UnoRankTheme(false) {
                UnoRankApp(AuthUiState(AuthScreen.Recovery, error = "Brak połączenia"), { _, _, _ -> }, { retried = true }, {}, {})
            }
        }
        compose.onNodeWithText("Brak połączenia").assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        assertTrue(retried)
        screenshot("recovery-light")
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun largeTextOnSmallScreenKeepsSettingsAndSubmitReachable() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
            UnoRankTheme(false) {
                UnoRankApp(AuthUiState(AuthScreen.Login, "http://127.0.0.1:8000/api/v1/"), { _, _, _ -> }, {}, {}, {})
            }
        }
        }
        compose.onNodeWithText("Ustawienia połączenia").performScrollTo().performClick()
        compose.onNodeWithText("Adres serwera").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("Zaloguj się") and hasClickAction()).performScrollTo().assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val output = File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
