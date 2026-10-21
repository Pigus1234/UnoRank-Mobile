package pl.unorank.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import pl.unorank.mobile.auth.AuthScreen
import pl.unorank.mobile.auth.AuthUiState

@Composable
fun UnoRankApp(
    state: AuthUiState,
    onLogin: (String, String, String) -> Unit,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    onExit: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 480.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
                BrandHeader()
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    when (state.screen) {
                        AuthScreen.Checking -> {
                            SectionTitle("Witaj ponownie", "Sprawdzam konto", "Łączę się z serwerem UnoRank.")
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                        AuthScreen.Login -> LoginContent(state, onLogin)
                        AuthScreen.Recovery -> {
                            SectionTitle("Zapisana sesja", "Sprawdź połączenie", "Aby otworzyć menu, musimy potwierdzić dostęp do Twojego konta.")
                            ErrorMessage(state.error)
                            PrimaryAction("Spróbuj ponownie", state.busy, onClick = onRetry)
                            TextButton(onClick = onLogout, enabled = !state.busy, modifier = Modifier.fillMaxWidth(), colors = textButtonColors()) {
                                Text("Wyloguj urządzenie")
                            }
                        }
                        AuthScreen.Home -> HomeContent(state, onLogout, onExit)
                    }
                    Text("UnoRank • Wersja 0.1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(UnoPink, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Text("U", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("UnoRank", fontWeight = FontWeight.Bold, fontSize = 21.sp)
                    Text("Panel administratora", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .25f))
        }
    }
}

@Composable
private fun LoginContent(state: AuthUiState, onLogin: (String, String, String) -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var server by rememberSaveable(state.server) { mutableStateOf(state.server) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val canSubmit = !state.busy && username.isNotBlank() && password.isNotEmpty() && server.isNotBlank()
    val submit = {
        if (canSubmit) {
            focus.clearFocus()
            val submittedPassword = password
            password = ""
            visible = false
            onLogin(server, username, submittedPassword)
        }
    }
    SectionTitle("Witaj w UnoRank", "Zaloguj się", "Dostęp dla administratora rozgrywek.")
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = username, onValueChange = { if (it.length <= 60) username = it },
            label = { Text("Login") }, singleLine = true, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false, imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = password, onValueChange = { if (it.length <= 255) password = it },
            label = { Text("Hasło") }, singleLine = true, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            trailingIcon = {
                TextButton(onClick = { visible = !visible }, enabled = !state.busy, colors = textButtonColors()) { Text(if (visible) "Ukryj" else "Pokaż") }
            },
        )
    }
    ErrorMessage(state.error)
    PrimaryAction("Zaloguj się", state.busy, canSubmit, submit)
    Text("Logowanie wymaga połączenia z serwerem.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick = { settingsOpen = !settingsOpen }, enabled = !state.busy, colors = textButtonColors(), contentPadding = PaddingValues(0.dp)) {
        Text(if (settingsOpen) "Ukryj ustawienia połączenia" else "Ustawienia połączenia")
    }
    if (settingsOpen) {
        OutlinedTextField(value = server, onValueChange = { server = it }, label = { Text("Adres serwera") }, singleLine = true,
            enabled = !state.busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false))
        Text("Zmień adres, jeśli korzystasz z innego serwera UnoRank.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HomeContent(state: AuthUiState, onLogout: () -> Unit, onExit: () -> Unit) {
    SectionTitle("Administrator: ${state.administrator}", "Gramy w UNO?", "To początek Twojej aplikacji.")
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Konto potwierdzone", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
            Text("Logowanie działa. Kolejne funkcje dodamy w następnych etapach.", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodySmall)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FutureTile("01", "Rozpocznij grę", "Nowa sesja i ustawienia stołu")
        FutureTile("02", "Użytkownicy", "Dodawanie i edycja graczy")
        FutureTile("03", "Historia rozgrywek", "Wyniki i archiwalne gry")
    }
    ErrorMessage(state.error)
    OutlinedButton(onClick = onLogout, enabled = !state.busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
        if (state.busy) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, strokeWidth = 2.dp) else Text("Wyloguj urządzenie")
    }
    TextButton(onClick = onExit, enabled = !state.busy, modifier = Modifier.fillMaxWidth(), colors = textButtonColors()) { Text("Wyjście") }
}

@Composable
private fun FutureTile(number: String, title: String, subtitle: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(number, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Wkrótce", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun SectionTitle(eyebrow: String, title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(eyebrow, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorMessage(error: String?) {
    if (error != null) {
        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
            Text(error, modifier = Modifier.padding(14.dp).semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PrimaryAction(label: String, busy: Boolean, enabled: Boolean = !busy, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled && !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
        if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp) else Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun textButtonColors() = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginPreview() {
    UnoRankTheme(darkTheme = true) { UnoRankApp(AuthUiState(AuthScreen.Login, "http://127.0.0.1:8000/api/v1/"), { _, _, _ -> }, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomePreview() {
    UnoRankTheme(darkTheme = true) { UnoRankApp(AuthUiState(AuthScreen.Home, administrator = "administrator"), { _, _, _ -> }, {}, {}, {}) }
}
