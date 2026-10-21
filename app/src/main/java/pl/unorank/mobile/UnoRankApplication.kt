package pl.unorank.mobile

import android.app.Application
import android.os.Build
import pl.unorank.mobile.auth.AuthApi
import pl.unorank.mobile.auth.AuthRepository
import pl.unorank.mobile.auth.Authentication
import pl.unorank.mobile.auth.EncryptedSessionStore

class UnoRankApplication : Application() {
    val authentication: Authentication by lazy {
        AuthRepository(
            AuthApi(),
            EncryptedSessionStore(this, if (BuildConfig.DEBUG) "http://127.0.0.1:8000/api/v1/" else ""),
            BuildConfig.DEBUG,
            "${Build.MANUFACTURER} ${Build.MODEL}",
        )
    }
}
