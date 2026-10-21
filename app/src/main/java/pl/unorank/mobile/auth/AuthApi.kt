package pl.unorank.mobile.auth

import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AuthApi(private val client: OkHttpClient = defaultClient()) {
    suspend fun login(server: ServerAddress, username: String, password: String, deviceName: String): SavedSession {
        val body = JSONObject().put("username", username).put("password", password).put("device_name", deviceName)
        val result = request(server, "auth/login", body = body, login = true)
        return parseResponse {
            val token = result.getString("token")
            val expires = Instant.parse(result.getString("expires_at"))
            if (token.isBlank() || token.contains('\n') || token.contains('\r') || !expires.isAfter(Instant.now())) {
                throw IllegalArgumentException()
            }
            SavedSession(server.value, token, expires)
        }
    }

    suspend fun me(server: ServerAddress, token: String): Administrator = parseResponse {
        val data = request(server, "auth/me", token).getJSONObject("data")
        val administrator = Administrator(data.getLong("id"), data.getString("username"))
        require(administrator.id > 0 && administrator.username.isNotBlank())
        administrator
    }

    suspend fun logout(server: ServerAddress, token: String) {
        request(server, "auth/logout", token, JSONObject())
    }

    private suspend fun request(
        server: ServerAddress,
        path: String,
        token: String? = null,
        body: JSONObject? = null,
        login: Boolean = false,
    ): JSONObject = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(server.url.resolve(path)!!).header("Accept", "application/json")
        token?.let { builder.header("Authorization", "Bearer $it") }
        body?.let { builder.post(it.toString().toRequestBody("application/json; charset=utf-8".toMediaType())) }
        try {
            client.newCall(builder.build()).execute().use { response ->
                when (response.code) {
                    401 -> throw AuthFailure(if (login) "Nieprawidłowy login lub hasło." else "Sesja wygasła. Zaloguj się ponownie.", !login)
                    403 -> throw AuthFailure("To konto nie ma dostępu administratora.", true)
                    422 -> throw AuthFailure("Sprawdź login i hasło. Serwer odrzucił podane dane.")
                    429 -> throw AuthFailure("Zbyt wiele prób. Poczekaj chwilę i spróbuj ponownie.")
                }
                if (!response.isSuccessful) throw AuthFailure("Serwer jest chwilowo niedostępny. Spróbuj ponownie.")
                if (response.code == 204) return@withContext JSONObject()
                val text = response.peekBody(65_537).string()
                if (text.length > 65_536) throw AuthFailure("Serwer zwrócił nieprawidłową odpowiedź.")
                parseResponse { JSONObject(text) }
            }
        } catch (failure: IOException) {
            throw AuthFailure("Nie udało się połączyć z serwerem. Sprawdź połączenie i adres serwera.")
        }
    }

    private inline fun <T> parseResponse(block: () -> T): T = try {
        block()
    } catch (failure: AuthFailure) {
        throw failure
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        throw AuthFailure("Serwer zwrócił nieprawidłową odpowiedź.")
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
}
