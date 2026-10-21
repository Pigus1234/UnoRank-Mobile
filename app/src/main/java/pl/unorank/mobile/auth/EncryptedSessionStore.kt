package pl.unorank.mobile.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

class EncryptedSessionStore(context: Context, private val defaultServer: String) : SessionStore {
    private val preferences = context.getSharedPreferences("unorank_session", Context.MODE_PRIVATE)
    private val keyAlias = "unorank_session_key"

    override fun load(): SavedSession? {
        val encoded = preferences.getString("session", null) ?: return null
        return try {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            val data = JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
            SavedSession(data.getString("server"), data.getString("token"), Instant.parse(data.getString("expires_at")))
        } catch (failure: Exception) {
            clear()
            null
        }
    }

    override fun save(session: SavedSession) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val data = JSONObject().put("server", session.server).put("token", session.token).put("expires_at", session.expiresAt.toString())
        val encrypted = cipher.iv + cipher.doFinal(data.toString().toByteArray(Charsets.UTF_8))
        if (!preferences.edit().putString("session", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()) {
            throw AuthFailure("Nie udało się zapisać sesji na telefonie. Sprawdź wolne miejsce.")
        }
    }

    override fun clear() {
        if (!preferences.edit().remove("session").commit()) throw AuthFailure("Nie udało się usunąć sesji z telefonu.")
    }

    override fun server(): String = preferences.getString("server", defaultServer) ?: defaultServer

    override fun saveServer(server: String) {
        if (!preferences.edit().putString("server", server).commit()) throw AuthFailure("Nie udało się zapisać adresu serwera.")
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build())
        }.generateKey()
    }
}
