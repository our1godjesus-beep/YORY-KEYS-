package app.yory.keys.ime

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Opt-in, device-local personal dictionary.
 * The file is encrypted with an AES-GCM key held by Android Keystore.
 * It is excluded from cloud sync and Android backup.
 */
object PersonalDictionaryStore {
    private const val PREFS = "yory_local_preferences"
    private const val ENABLED = "personal_learning_enabled"
    private const val KEY_ALIAS = "yory_personal_dictionary_aes_v1"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val MAX_WORDS_PER_LANGUAGE = 1500
    private const val MAX_COUNT = 100
    @Volatile private var cachedDocument: JSONObject? = null
    @Volatile private var rankedWordCache: Map<String, List<Pair<String, Int>>> = emptyMap()

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(ENABLED, enabled)
            .apply()
    }

    @Synchronized
    fun learn(context: Context, rawWord: String, languageTag: String) {
        if (!isEnabled(context)) return
        val word = rawWord.trim().lowercase()
        if (word.length !in 2..32 || !WORD.matches(word)) return
        val language = languageTag.substringBefore('-').lowercase().take(12)
        if (language.isBlank()) return

        val root = readDocument(context)
        val languageWords = root.optJSONObject(language) ?: JSONObject()
        val previous = languageWords.optInt(word, 0)
        languageWords.put(word, (previous + 1).coerceAtMost(MAX_COUNT))
        rankedWordCache = rankedWordCache - language

        val retained = languageWords.keys().asSequence()
            .map { key -> key to languageWords.optInt(key, 0) }
            .sortedByDescending { it.second }
            .take(MAX_WORDS_PER_LANGUAGE)
            .toList()
        val bounded = JSONObject()
        retained.forEach { (key, count) -> bounded.put(key, count) }
        root.put(language, bounded)
        writeDocument(context, root)
    }

    @Synchronized
    fun suggestions(context: Context, prefix: String, languageTag: String, limit: Int = 3): List<String> {
        if (!isEnabled(context)) return emptyList()
        val clean = prefix.trim().lowercase()
        if (clean.length < 2) return emptyList()
        val language = languageTag.substringBefore('-').lowercase().take(12)
        val words = readDocument(context).optJSONObject(language) ?: return emptyList()
        val ranked = rankedWordCache[language] ?: words.keys().asSequence()
            .map { it to words.optInt(it, 0) }
            .sortedByDescending { it.second }
            .toList()
            .also { rankedWordCache = rankedWordCache + (language to it) }
        return ranked.asSequence()
            .map { it.first }
            .filter { YorySuggestionText.matchesPrefix(it, clean) }
            .take(limit.coerceIn(1, 5))
            .toList()
    }

    @Synchronized
    fun clear(context: Context) {
        cachedDocument = null
        rankedWordCache = emptyMap()
        runCatching { dataFile(context).delete() }
        runCatching {
            val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS)
        }
    }

    private fun dataFile(context: Context) =
        AtomicFile(File(context.noBackupFilesDir, "yory_personal_dictionary.enc"))

    private fun readDocument(context: Context): JSONObject {
        cachedDocument?.let { return it }
        val file = dataFile(context)
        if (!file.baseFile.exists()) return JSONObject().also { cachedDocument = it }
        val document = runCatching {
            val encrypted = file.openRead().use { it.readBytes() }
            require(encrypted.size > 12) { "Personal dictionary file is incomplete." }
            val iv = encrypted.copyOfRange(0, 12)
            val ciphertext = encrypted.copyOfRange(12, encrypted.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            JSONObject(String(cipher.doFinal(ciphertext), Charsets.UTF_8))
        }.getOrElse {
            // Corrupt or undecryptable local data must never crash the keyboard.
            runCatching { file.delete() }
            JSONObject()
        }
        cachedDocument = document
        return document
    }

    private fun writeDocument(context: Context, document: JSONObject) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.iv + cipher.doFinal(document.toString().toByteArray(Charsets.UTF_8))
        val file = dataFile(context)
        val stream = file.startWrite()
        try {
            stream.write(encrypted)
            file.finishWrite(stream)
            cachedDocument = document
            rankedWordCache = emptyMap()
        } catch (error: Throwable) {
            file.failWrite(stream)
            throw error
        }
    }

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

    private val WORD = Regex("[\\p{L}\\p{M}]{2,32}")
}
