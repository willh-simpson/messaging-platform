package com.messaging.messagingplatform.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.messaging.messagingplatform.data.api.AuthApi
import com.messaging.messagingplatform.data.model.LoginRequest
import com.messaging.messagingplatform.data.model.RegisterRequest
import com.messaging.messagingplatform.domain.model.AuthResult
import com.messaging.messagingplatform.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import androidx.core.content.edit

/**
 * Handles login/register and JWT persistence.
 */
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    @ApplicationContext private val context: Context,
) : AuthRepository {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS    = "messaging_platform_auth_key"
        private const val PREFS_FILENAME    = "secure_auth_prefs"
        private const val CIPHER_TRANSFORM  = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH    = 128  // max available bits

        // SharedPreferences keys. Each sensitive value gets its own IV key.
        private const val KEY_TOKEN         = "enc_token"
        private const val KEY_TOKEN_IV      = "enc_token_iv"
        private const val KEY_USER_ID       = "enc_user_id"
        private const val KEY_USER_ID_IV    = "enc_user_id_iv"
        private const val KEY_USERNAME      = "enc_username"
        private const val KEY_USERNAME_IV   = "enc_username_iv"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns AES secret key from AndroidKeyStore.
     * Generates a key if it doesn't exist (e.g., first app launch, user clears app data, etc.)
     */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        keyStore.getKey(KEYSTORE_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true) // rejects any IV reuse
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts a plaintext string.
     * IV must be stored alongside the ciphertext for decryption.
     */
    private fun encrypt(plaintext: String): Pair<String, String> {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv

        return Pair(
            Base64.encodeToString(ciphertext, Base64.DEFAULT),
            Base64.encodeToString(iv, Base64.DEFAULT)
        )
    }

    /**
     * Decrypts a Base64-encoded ciphertext with the stored IV.
     * If value is missing or decryption fails then returns null.
     */
    private fun decrypt(ciphertextB64: String?, ivB64: String?): String? {
        if (ciphertextB64 == null || ivB64 == null) return null

        return try {
            val ciphertext = Base64.decode(ciphertextB64, Base64.DEFAULT)
            val iv = Base64.decode(ivB64, Base64.DEFAULT)

            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            /*
             * returning null forces user to sign in again instead of serving corrupt auth data
             * or cascading errors.
             */
            null
        }
    }

    override suspend fun login(username: String, password: String): Result<AuthResult> =
        runCatching {
            val data = authApi.login(LoginRequest(username, password)).data

            AuthResult(
                token = data.token,
                userId = data.userId,
                username = data.username,
            )
        }

    override suspend fun register(
        username: String,
        email: String,
        password: String,
    ): Result<AuthResult> = runCatching {
        val data = authApi.register(RegisterRequest(username, email, password)).data

        AuthResult(
            token = data.token,
            userId = data.userId,
            username = data.username,
        )
    }

    override suspend fun saveToken(
        token: String,
        userId: String,
        username: String
    ) {
        val (encToken, ivToken) = encrypt(token)
        val (encUserId, ivUserId) = encrypt(userId)
        val (encUsername, ivUsername) = encrypt(username)

        prefs.edit {
            putString(KEY_TOKEN, encToken)
                .putString(KEY_TOKEN_IV, ivToken)
                .putString(KEY_USER_ID, encUserId)
                .putString(KEY_USER_ID_IV, ivUserId)
                .putString(KEY_USERNAME, encUsername)
                .putString(KEY_USERNAME_IV, ivUsername)
        }
    }

    override suspend fun getToken(): String? =
        decrypt(
            prefs.getString(KEY_TOKEN, null),
            prefs.getString(KEY_TOKEN_IV, null),
        )

    override suspend fun getUserId(): String? =
        decrypt(
            prefs.getString(KEY_USER_ID,    null),
            prefs.getString(KEY_USER_ID_IV, null),
        )

    override suspend fun getUsername(): String? =
        decrypt(
            prefs.getString(KEY_USERNAME,    null),
            prefs.getString(KEY_USERNAME_IV, null),
        )

    override suspend fun clearSession() {
        prefs.edit { clear() }
    }
}