package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppThemeMode
import com.example.domain.model.SecurityLockTimeout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ledgerly_settings")

data class UserPreferences(
    val currency: String = "INR",
    val startDayOfMonth: Int = 1,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accentColorHex: String = "#10B981",
    val isOnboardingCompleted: Boolean = false,
    val isPinLockEnabled: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val hideSensitiveBalances: Boolean = false,
    val pinHash: String = "",
    val isBiometricEnabled: Boolean = false,
    val lockTimeout: SecurityLockTimeout = SecurityLockTimeout.IMMEDIATELY,
    val hideAppPreview: Boolean = false,
    val decimalPlaces: Int = 2,
    val isCompactMode: Boolean = false,
    val enableAnimations: Boolean = true,
    val lastUnlockedTimestamp: Long = 0L
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val CURRENCY = stringPreferencesKey("currency")
        val START_DAY_OF_MONTH = intPreferencesKey("start_day_of_month")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR_HEX = stringPreferencesKey("accent_color_hex")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val IS_PIN_LOCK_ENABLED = booleanPreferencesKey("is_pin_lock_enabled")
        val IS_APP_LOCK_ENABLED = booleanPreferencesKey("is_app_lock_enabled")
        val HIDE_SENSITIVE_BALANCES = booleanPreferencesKey("hide_sensitive_balances")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
        val LOCK_TIMEOUT = stringPreferencesKey("lock_timeout")
        val HIDE_APP_PREVIEW = booleanPreferencesKey("hide_app_preview")
        val DECIMAL_PLACES = intPreferencesKey("decimal_places")
        val IS_COMPACT_MODE = booleanPreferencesKey("is_compact_mode")
        val ENABLE_ANIMATIONS = booleanPreferencesKey("enable_animations")
        val LAST_UNLOCKED_TIMESTAMP = longPreferencesKey("last_unlocked_timestamp")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val currency = preferences[PreferencesKeys.CURRENCY] ?: "INR"
        val startDayOfMonth = preferences[PreferencesKeys.START_DAY_OF_MONTH] ?: 1
        val themeModeStr = preferences[PreferencesKeys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
        val themeMode = runCatching { AppThemeMode.valueOf(themeModeStr) }.getOrDefault(AppThemeMode.SYSTEM)
        val accentColorHex = preferences[PreferencesKeys.ACCENT_COLOR_HEX] ?: "#10B981"
        val isOnboardingCompleted = preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: false
        val isPinLockEnabled = preferences[PreferencesKeys.IS_PIN_LOCK_ENABLED] ?: false
        val isAppLockEnabled = preferences[PreferencesKeys.IS_APP_LOCK_ENABLED] ?: isPinLockEnabled
        val hideSensitiveBalances = preferences[PreferencesKeys.HIDE_SENSITIVE_BALANCES] ?: false
        val pinHash = preferences[PreferencesKeys.PIN_HASH] ?: ""
        val isBiometricEnabled = preferences[PreferencesKeys.IS_BIOMETRIC_ENABLED] ?: false
        val lockTimeoutStr = preferences[PreferencesKeys.LOCK_TIMEOUT] ?: SecurityLockTimeout.IMMEDIATELY.name
        val lockTimeout = runCatching { SecurityLockTimeout.valueOf(lockTimeoutStr) }.getOrDefault(SecurityLockTimeout.IMMEDIATELY)
        val hideAppPreview = preferences[PreferencesKeys.HIDE_APP_PREVIEW] ?: false
        val decimalPlaces = preferences[PreferencesKeys.DECIMAL_PLACES] ?: 2
        val isCompactMode = preferences[PreferencesKeys.IS_COMPACT_MODE] ?: false
        val enableAnimations = preferences[PreferencesKeys.ENABLE_ANIMATIONS] ?: true
        val lastUnlockedTimestamp = preferences[PreferencesKeys.LAST_UNLOCKED_TIMESTAMP] ?: 0L

        UserPreferences(
            currency = currency,
            startDayOfMonth = startDayOfMonth,
            themeMode = themeMode,
            accentColorHex = accentColorHex,
            isOnboardingCompleted = isOnboardingCompleted,
            isPinLockEnabled = isPinLockEnabled,
            isAppLockEnabled = isAppLockEnabled,
            hideSensitiveBalances = hideSensitiveBalances,
            pinHash = pinHash,
            isBiometricEnabled = isBiometricEnabled,
            lockTimeout = lockTimeout,
            hideAppPreview = hideAppPreview,
            decimalPlaces = decimalPlaces,
            isCompactMode = isCompactMode,
            enableAnimations = enableAnimations,
            lastUnlockedTimestamp = lastUnlockedTimestamp
        )
    }

    suspend fun updateAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.IS_APP_LOCK_ENABLED] = enabled }
    }

    suspend fun updateHideSensitiveBalances(hide: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HIDE_SENSITIVE_BALANCES] = hide }
    }

    suspend fun updateCurrency(currency: String) {
        context.dataStore.edit { it[PreferencesKeys.CURRENCY] = currency }
    }

    suspend fun updateStartDayOfMonth(day: Int) {
        context.dataStore.edit { it[PreferencesKeys.START_DAY_OF_MONTH] = day }
    }

    suspend fun updateThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode.name }
    }

    suspend fun updateAccentColor(hex: String) {
        context.dataStore.edit { it[PreferencesKeys.ACCENT_COLOR_HEX] = hex }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.IS_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setPinLock(pin: String?) {
        context.dataStore.edit { preferences ->
            if (pin.isNullOrBlank()) {
                preferences[PreferencesKeys.IS_PIN_LOCK_ENABLED] = false
                preferences[PreferencesKeys.PIN_HASH] = ""
            } else {
                preferences[PreferencesKeys.IS_PIN_LOCK_ENABLED] = true
                preferences[PreferencesKeys.PIN_HASH] = hashPin(pin)
            }
        }
    }

    fun verifyPin(inputPin: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return true
        return hashPin(inputPin) == storedHash
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.IS_BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setLockTimeout(timeout: SecurityLockTimeout) {
        context.dataStore.edit { it[PreferencesKeys.LOCK_TIMEOUT] = timeout.name }
    }

    suspend fun setHideAppPreview(hide: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HIDE_APP_PREVIEW] = hide }
    }

    suspend fun setDecimalPlaces(places: Int) {
        context.dataStore.edit { it[PreferencesKeys.DECIMAL_PLACES] = places }
    }

    suspend fun setCompactMode(compact: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.IS_COMPACT_MODE] = compact }
    }

    suspend fun setEnableAnimations(enable: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.ENABLE_ANIMATIONS] = enable }
    }

    suspend fun updateLastUnlockedTimestamp(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { it[PreferencesKeys.LAST_UNLOCKED_TIMESTAMP] = timestamp }
    }
}
