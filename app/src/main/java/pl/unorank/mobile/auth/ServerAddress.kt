package pl.unorank.mobile.auth

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class ServerAddress private constructor(val url: HttpUrl) {
    val value: String get() = url.toString()

    companion object {
        fun parse(value: String, allowLocalHttp: Boolean): ServerAddress {
            val url = value.trim().toHttpUrlOrNull()
                ?: throw AuthFailure("Wpisz poprawny adres serwera.")
            val localHttp = allowLocalHttp && url.host in setOf("127.0.0.1", "localhost", "10.0.2.2")
            if (url.scheme != "https" && !localHttp) {
                throw AuthFailure("Ten adres wymaga bezpiecznego połączenia HTTPS.")
            }
            if (url.username.isNotEmpty() || url.password.isNotEmpty() || url.query != null || url.fragment != null) {
                throw AuthFailure("Adres serwera nie może zawierać hasła, parametrów ani fragmentu.")
            }
            val path = url.encodedPath.trimEnd('/')
            if (path.isNotEmpty() && path != "/api/v1") {
                throw AuthFailure("Podaj adres serwera bez ścieżki lub zakończony /api/v1.")
            }
            return ServerAddress(url.newBuilder().encodedPath("/api/v1/").build())
        }
    }
}
