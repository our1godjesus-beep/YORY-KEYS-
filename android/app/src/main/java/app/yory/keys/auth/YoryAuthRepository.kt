package app.yory.keys.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.yory.keys.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

sealed class YoryAuthResult {
    data class SignedIn(val email: String) : YoryAuthResult()
    data class NeedsEmailVerification(val email: String) : YoryAuthResult()
}

class YoryAuthException(message: String) : Exception(message)

data class YoryAuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val email: String
)

object YoryAuthRepository {
    private const val SESSION_PREFS = "yory_secure_auth_session"
    private const val SESSION_KEY = "encrypted_session"
    private const val KEY_ALIAS = "app.yory.keys.auth.session"
    private val gson = Gson()
    private var applicationContextForStore: Context? = null

    fun initialize(context: Context) {
        applicationContextForStore = context.applicationContext
    }

    private fun requireConfig(): Pair<String, String> {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
        if (!baseUrl.startsWith("https://") || publishableKey.isBlank()) {
            throw YoryAuthException(
                "YORY account service is not connected yet. Add SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY to android/supabase.local.properties, or configure them as GitHub Actions repository secrets."
            )
        }
        return baseUrl to publishableKey
    }

    private fun request(path: String, payload: Map<String, Any?>, bearer: String? = null): JsonObject {
        val (baseUrl, publishableKey) = requireConfig()
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("apikey", publishableKey)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (!bearer.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $bearer")
        }
        return try {
            connection.outputStream.use { output ->
                output.write(gson.toJson(payload).toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = if (body.isBlank()) JsonObject() else JsonParser.parseString(body).asJsonObject
            if (status !in 200..299) {
                val message = sequenceOf("msg", "message", "error_description", "error")
                    .mapNotNull { json.get(it)?.takeIf { value -> !value.isJsonNull }?.asString }
                    .firstOrNull()
                    ?: "The account service rejected the request (HTTP $status)."
                throw YoryAuthException(message)
            }
            json
        } catch (error: YoryAuthException) {
            throw error
        } catch (error: Exception) {
            throw YoryAuthException(error.message?.takeIf { it.isNotBlank() } ?: "Could not connect to the account service.")
        } finally {
            connection.disconnect()
        }
    }

    suspend fun signUp(displayName: String, email: String, password: String): YoryAuthResult =
        withContext(Dispatchers.IO) {
            val response = request(
                "/auth/v1/signup",
                mapOf(
                    "email" to email.trim(),
                    "password" to password,
                    "data" to mapOf("display_name" to displayName.trim())
                )
            )
            val session = sessionFrom(response, email)
            if (session != null) {
                saveSession(session)
                YoryAuthResult.SignedIn(session.email)
            } else {
                YoryAuthResult.NeedsEmailVerification(email.trim())
            }
        }

    suspend fun signIn(email: String, password: String): YoryAuthResult =
        withContext(Dispatchers.IO) {
            val response = request(
                "/auth/v1/token?grant_type=password",
                mapOf("email" to email.trim(), "password" to password)
            )
            val session = sessionFrom(response, email)
                ?: throw YoryAuthException("Sign-in did not return a valid session.")
            saveSession(session)
            YoryAuthResult.SignedIn(session.email)
        }

    suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        val current = loadSession() ?: return@withContext false
        if (current.expiresAtEpochSeconds > (System.currentTimeMillis() / 1000L) + 60) {
            return@withContext true
        }
        runCatching {
            val response = request(
                "/auth/v1/token?grant_type=refresh_token",
                mapOf("refresh_token" to current.refreshToken)
            )
            val refreshed = sessionFrom(response, current.email)
                ?: throw YoryAuthException("The saved session could not be refreshed.")
            saveSession(refreshed)
            true
        }.getOrElse {
            clearSession()
            false
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val current = loadSession()
        if (current != null) runCatching {
            request("/auth/v1/logout", emptyMap(), current.accessToken)
        }
        clearSession()
    }

    private fun sessionFrom(json: JsonObject, fallbackEmail: String): YoryAuthSession? {
        val access = json.get("access_token")?.takeIf { !it.isJsonNull }?.asString ?: return null
        val refresh = json.get("refresh_token")?.takeIf { !it.isJsonNull }?.asString ?: return null
        val expiresIn = json.get("expires_in")?.takeIf { !it.isJsonNull }?.asLong ?: 3600L
        val userEmail = json.getAsJsonObject("user")?.get("email")
            ?.takeIf { !it.isJsonNull }?.asString ?: fallbackEmail
        return YoryAuthSession(
            accessToken = access,
            refreshToken = refresh,
            expiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + expiresIn,
            email = userEmail
        )
    }

    private fun saveSession(session: YoryAuthSession) {
        val context = applicationContextForStore ?: throw YoryAuthException("Secure session storage is unavailable.")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(gson.toJson(session).toByteArray(Charsets.UTF_8))
        val packed = byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + encrypted
        context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
            .edit().putString(SESSION_KEY, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    private fun loadSession(): YoryAuthSession? {
        val context = applicationContextForStore ?: return null
        val stored = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
            .getString(SESSION_KEY, null) ?: return null
        return try {
            val packed = Base64.decode(stored, Base64.NO_WRAP)
            val ivLength = packed.first().toInt()
            require(ivLength in 12..16 && packed.size > ivLength + 1)
            val iv = packed.copyOfRange(1, 1 + ivLength)
            val encrypted = packed.copyOfRange(1 + ivLength, packed.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            gson.fromJson(String(cipher.doFinal(encrypted), Charsets.UTF_8), YoryAuthSession::class.java)
        } catch (_: Exception) {
            clearSession()
            null
        }
    }

    private fun clearSession() {
        applicationContextForStore?.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
            ?.edit()?.remove(SESSION_KEY)?.apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
