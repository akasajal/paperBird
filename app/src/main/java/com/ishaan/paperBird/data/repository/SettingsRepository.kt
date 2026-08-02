package com.ishaan.paperBird.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.ishaan.paperBird.domain.model.CATEGORY_COLORS
import com.ishaan.paperBird.domain.model.DEFAULT_CATEGORIES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_DARK_THEME = stringPreferencesKey("dark_theme")
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        val KEY_EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size")
        val KEY_DEFAULT_CATEGORY = stringPreferencesKey("default_category")
        val KEY_CUSTOM_CATEGORIES = stringPreferencesKey("custom_categories")
        val KEY_CUSTOM_CATEGORY_COLORS = stringPreferencesKey("custom_category_colors")

        val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val KEY_USE_BIOMETRICS = booleanPreferencesKey("use_biometrics")
        val KEY_USE_PIN = booleanPreferencesKey("use_pin")
        val KEY_APP_PIN_HASH = stringPreferencesKey("app_pin_hash")
        val KEY_INSTANT_LOCK = booleanPreferencesKey("instant_lock")
    }

    val darkTheme: Flow<Boolean?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map {
            when (it[KEY_DARK_THEME]) {
                "dark"  -> true
                "light" -> false
                else    -> null
            }
        }

    val accentColor: Flow<String> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_ACCENT_COLOR] ?: "Rose" }

    val editorFontSize: Flow<Int> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_EDITOR_FONT_SIZE] ?: 16 }

    val defaultCategory: Flow<String> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_DEFAULT_CATEGORY] ?: "Today" }

    val customCategories: Flow<List<String>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map {
            val raw = it[KEY_CUSTOM_CATEGORIES] ?: ""
            if (raw.isBlank()) emptyList()
            else raw.split(",").filter(String::isNotBlank)
        }

    val customCategoryColors: Flow<Map<String, Long>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val raw = prefs[KEY_CUSTOM_CATEGORY_COLORS] ?: "{}"
            try {
                val json = JSONObject(raw)
                val map = mutableMapOf<String, Long>()
                json.keys().forEach { key -> map[key] = json.getLong(key) }
                map
            } catch (e: Exception) { emptyMap() }
        }

    val allCategories: Flow<List<String>> = customCategories.map { custom ->
        DEFAULT_CATEGORIES + custom.filter { it !in DEFAULT_CATEGORIES }
    }

    val allCategoryColors: Flow<Map<String, Long>> = customCategoryColors.map { custom ->
        CATEGORY_COLORS + custom
    }

    val appLockEnabled: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_APP_LOCK_ENABLED] ?: false }

    val useBiometrics: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_USE_BIOMETRICS] ?: false }

    val usePin: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_USE_PIN] ?: false }

    val appPinHash: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_APP_PIN_HASH] }

    val instantLock: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_INSTANT_LOCK] ?: false }

    suspend fun setDarkTheme(value: Boolean) =
        dataStore.edit { it[KEY_DARK_THEME] = if (value) "dark" else "light" }

    suspend fun setSystemTheme() = dataStore.edit { it.remove(KEY_DARK_THEME) }

    suspend fun setAccentColor(value: String) = dataStore.edit { it[KEY_ACCENT_COLOR] = value }
    suspend fun setEditorFontSize(value: Int) = dataStore.edit { it[KEY_EDITOR_FONT_SIZE] = value }
    suspend fun setDefaultCategory(value: String) = dataStore.edit { it[KEY_DEFAULT_CATEGORY] = value }

    suspend fun addCustomCategory(name: String, color: Long) = dataStore.edit { prefs ->
        val existing = prefs[KEY_CUSTOM_CATEGORIES]?.split(",")?.filter(String::isNotBlank) ?: emptyList()
        if (name !in existing && name !in DEFAULT_CATEGORIES) {
            prefs[KEY_CUSTOM_CATEGORIES] = (existing + name).joinToString(",")
            val rawColors = prefs[KEY_CUSTOM_CATEGORY_COLORS] ?: "{}"
            val json = JSONObject(rawColors)
            json.put(name, color)
            prefs[KEY_CUSTOM_CATEGORY_COLORS] = json.toString()
        }
    }

    suspend fun removeCustomCategory(name: String) = dataStore.edit { prefs ->
        val existing = prefs[KEY_CUSTOM_CATEGORIES]?.split(",")?.filter(String::isNotBlank) ?: emptyList()
        prefs[KEY_CUSTOM_CATEGORIES] = existing.filter { it != name }.joinToString(",")
        val rawColors = prefs[KEY_CUSTOM_CATEGORY_COLORS] ?: "{}"
        try {
            val json = JSONObject(rawColors)
            json.remove(name)
            prefs[KEY_CUSTOM_CATEGORY_COLORS] = json.toString()
        } catch (_: Exception) {}
    }

    suspend fun setAppLockEnabled(value: Boolean) = dataStore.edit { it[KEY_APP_LOCK_ENABLED] = value }
    suspend fun setUseBiometrics(value: Boolean) = dataStore.edit { it[KEY_USE_BIOMETRICS] = value }
    suspend fun setUsePin(value: Boolean) = dataStore.edit { it[KEY_USE_PIN] = value }
    suspend fun setInstantLock(value: Boolean) = dataStore.edit { it[KEY_INSTANT_LOCK] = value }

    /** Enables app lock: stores the PIN hash and flips the enabled flag atomically. */
    suspend fun enableAppLock(pin: String) = dataStore.edit {
        it[KEY_APP_PIN_HASH] = hashPin(pin)
        it[KEY_USE_PIN] = true
        it[KEY_APP_LOCK_ENABLED] = true
    }

    /** Disables app lock: clears PIN hash, biometrics, instant lock, and the enabled flag. */
    suspend fun disableAppLock() = dataStore.edit {
        it[KEY_APP_LOCK_ENABLED] = false
        it[KEY_USE_PIN] = false
        it[KEY_USE_BIOMETRICS] = false
        it[KEY_INSTANT_LOCK] = false
        it.remove(KEY_APP_PIN_HASH)
    }

    suspend fun setAppPin(pin: String) = dataStore.edit {
        it[KEY_APP_PIN_HASH] = hashPin(pin)
        it[KEY_USE_PIN] = true
    }

    fun verifyPin(input: String, storedHash: String?): Boolean = hashPin(input) == storedHash

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(pin.toByteArray()).fold("") { s, b -> s + "%02x".format(b) }
    }
}