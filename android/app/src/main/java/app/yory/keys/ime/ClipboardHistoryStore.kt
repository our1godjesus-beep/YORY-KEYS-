package app.yory.keys.ime

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class YoryClipboardEntry(
    val id: String,
    val text: String,
    val copiedAt: Long,
    val isPinned: Boolean = false,
    val expiresAt: Long = 0L
)

/**
 * Local-only clipboard history encrypted with Android Keystore.
 * Unpinned clips expire after 24 hours. PIN/OTP-like values are rejected.
 */
object ClipboardHistoryStore {
    private const val LEGACY_PREFS = "yory_clipboard_history"
    private const val LEGACY_KEY = "items"
    private const val KEY_ALIAS = "yory_clipboard_aes_v1"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val MAX_ITEMS = 50
    private const val MAX_TEXT_LENGTH = 2000
    private const val EXPIRY_MS = 24L * 60L * 60L * 1000L
    private val lock = Any()

    fun entries(context: Context): List<YoryClipboardEntry> = synchronized(lock) {
        runCatching {
            val now = System.currentTimeMillis()
            val all = readAll(context)
            val valid = all.filter { it.isPinned || it.expiresAt > now }
            if (valid.size != all.size) writeAll(context, valid)
            valid.sortedWith(compareByDescending<YoryClipboardEntry> { it.isPinned }.thenByDescending { it.copiedAt })
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, rawText: String): Boolean = synchronized(lock) {
        val text = rawText.trim()
        if (!isSafeToStore(text)) return false
        runCatching {
            val now = System.currentTimeMillis()
            val current = entries(context).filterNot { it.text == text }
            val updated = listOf(
                YoryClipboardEntry(
                    id = UUID.randomUUID().toString(),
                    text = text,
                    copiedAt = now,
                    isPinned = false,
                    expiresAt = now + EXPIRY_MS
                )
            ) + current
            writeAll(context, updated.take(MAX_ITEMS))
        }.isSuccess
    }

    fun setPinned(context: Context, id: String, pinned: Boolean) = synchronized(lock) {
        runCatching {
            val now = System.currentTimeMillis()
            val updated = entries(context).map { entry ->
                if (entry.id == id) {
                    entry.copy(isPinned = pinned, expiresAt = if (pinned) Long.MAX_VALUE else now + EXPIRY_MS)
                } else entry
            }
            writeAll(context, updated)
        }
    }

    fun delete(context: Context, id: String): YoryClipboardEntry? = synchronized(lock) {
        val current = entries(context)
        val deleted = current.firstOrNull { it.id == id } ?: return null
        runCatching { writeAll(context, current.filterNot { it.id == id }) }
            .onFailure { return null }
        deleted
    }

    fun restore(context: Context, entry: YoryClipboardEntry) = synchronized(lock) {
        runCatching {
            val current = entries(context).filterNot { it.id == entry.id || it.text == entry.text }
            writeAll(context, (listOf(entry) + current).take(MAX_ITEMS))
        }
    }

    fun clear(context: Context) = synchronized(lock) {
        runCatching { dataFile(context).delete() }
        context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE).edit().remove(LEGACY_KEY).apply()
    }

    private fun readAll(context: Context): List<YoryClipboardEntry> {
        val file = dataFile(context)
        if (!file.baseFile.exists()) return migrateLegacy(context)
        return runCatching {
            val encrypted = file.openRead().use { it.readBytes() }
            require(encrypted.size > 12) { "Clipboard history is incomplete." }
            val iv = encrypted.copyOfRange(0, 12)
            val ciphertext = encrypted.copyOfRange(12, encrypted.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            val array = JSONArray(String(cipher.doFinal(ciphertext), Charsets.UTF_8))
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val text = item.optString("text").takeIf { it.isNotBlank() } ?: continue
                    add(
                        YoryClipboardEntry(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            text = text,
                            copiedAt = item.optLong("copiedAt"),
                            isPinned = item.optBoolean("pinned", false),
                            expiresAt = item.optLong("expiresAt", 0L)
                        )
                    )
                }
            }
        }.getOrElse {
            runCatching { file.delete() }
            emptyList()
        }
    }

    private fun migrateLegacy(context: Context): List<YoryClipboardEntry> {
        val prefs = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(LEGACY_KEY, null) ?: return emptyList()
        val now = System.currentTimeMillis()
        val migrated = runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val text = item.optString("text").takeIf { isSafeToStore(it) } ?: continue
                    val copiedAt = item.optLong("copiedAt", now)
                    add(
                        YoryClipboardEntry(
                            id = UUID.randomUUID().toString(),
                            text = text,
                            copiedAt = copiedAt,
                            isPinned = false,
                            expiresAt = now + EXPIRY_MS
                        )
                    )
                }
            }
        }.getOrDefault(emptyList()).take(MAX_ITEMS)
        if (migrated.isNotEmpty()) runCatching { writeAll(context, migrated) }
        prefs.edit().remove(LEGACY_KEY).apply()
        return migrated
    }

    private fun writeAll(context: Context, entries: List<YoryClipboardEntry>) {
        val array = JSONArray()
        entries.take(MAX_ITEMS).forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("text", entry.text)
                    .put("copiedAt", entry.copiedAt)
                    .put("pinned", entry.isPinned)
                    .put("expiresAt", entry.expiresAt)
            )
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.iv + cipher.doFinal(array.toString().toByteArray(Charsets.UTF_8))
        val file = dataFile(context)
        val stream = file.startWrite()
        try {
            stream.write(encrypted)
            file.finishWrite(stream)
        } catch (error: Throwable) {
            file.failWrite(stream)
            throw error
        }
    }

    private fun dataFile(context: Context) =
        AtomicFile(File(context.noBackupFilesDir, "yory_clipboard_history.enc"))

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun isSafeToStore(text: String): Boolean {
        if (text.isBlank() || text.length > MAX_TEXT_LENGTH) return false
        val compact = text.filterNot { it.isWhitespace() || it in "-()." }
        if (compact.matches(Regex("\\d{4,10}"))) return false
        if (compact.matches(Regex("\\d{12,19}"))) return false
        if (Regex("(?i)otp|one[- ]time|verification code|رمز التحقق|كود التحقق").containsMatchIn(text)) return false
        if (Regex("(?i)(password|passcode|secret|token)\\s*[:=]").containsMatchIn(text)) return false
        return true
    }
}
