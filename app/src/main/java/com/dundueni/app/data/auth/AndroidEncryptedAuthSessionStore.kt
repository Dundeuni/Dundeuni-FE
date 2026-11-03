package com.dundueni.app.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.dundueni.app.BuildConfig
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** AES-GCM protected SharedPreferences; the encryption key remains in Android Keystore. */
class AndroidEncryptedAuthSessionStore(context: Context) : AuthSessionStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized
    override fun read(): AuthSession? {
        val encrypted = preferences.getString(SESSION_KEY, null) ?: return null
        return runCatching {
            val pieces = encrypted.split(':', limit = 2)
            require(pieces.size == 2)
            val iv = Base64.decode(pieces[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(pieces[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
            val json = JSONObject(String(cipher.doFinal(ciphertext), Charsets.UTF_8))
            val source = AuthSessionSource.valueOf(json.getString("source"))
            val token = json.getString("accessToken")
            AuthSession(token, source).takeIf { it.expiresAtEpochSeconds() != null }
        }.getOrElse {
            preferences.edit().remove(SESSION_KEY).commit()
            null
        }
    }

    @Synchronized
    override fun save(session: AuthSession) {
        require(session.accessToken.isNotBlank() && session.isValid()) { "A non-expired JWT access token is required" }
        require(session.source != AuthSessionSource.MOCK || BuildConfig.DEBUG) { "Mock sessions are debug-only" }
        val plaintext = JSONObject()
            .put("accessToken", session.accessToken)
            .put("source", session.source.name)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plaintext)
        val value = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        check(preferences.edit().putString(SESSION_KEY, value).commit()) { "Unable to persist auth session" }
    }

    @Synchronized
    override fun clear() {
        preferences.edit().remove(SESSION_KEY).commit()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES = "auth_session"
        const val SESSION_KEY = "encrypted_session"
        const val KEY_ALIAS = "dundeuni_auth_session_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }
}
