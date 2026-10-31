package com.fitplanner.ai.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fitplanner.ai.core.data.AppMoshi
import com.fitplanner.ai.core.data.readJson
import com.fitplanner.ai.core.data.writeJson
import com.fitplanner.ai.core.model.AiMode
import com.fitplanner.ai.core.model.AiSettings
import com.fitplanner.ai.core.model.AppPreferences
import com.fitplanner.ai.core.model.OnboardingDraft
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private object Keys {
    val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
    val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
    val reminderTime = stringPreferencesKey("reminder_time")
    val themeMode = stringPreferencesKey("theme_mode")
    val aiMode = stringPreferencesKey("ai_mode")
    val aiBaseUrl = stringPreferencesKey("ai_base_url")
    val aiModel = stringPreferencesKey("ai_model")
    val aiConsent = booleanPreferencesKey("ai_consent")
    val onboardingDraft = stringPreferencesKey("onboarding_draft")
}

@Singleton
class SecureKeyStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "fitplanner_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrNull()
    }

    fun hasApiKey(): Boolean = !readApiKey().isNullOrBlank()

    fun readApiKey(): String? = runCatching { prefs?.getString("ai_api_key", null) }.getOrNull()

    fun saveApiKey(value: String) {
        runCatching { prefs?.edit()?.putString("ai_api_key", value)?.apply() }
    }

    fun clearApiKey() {
        runCatching { prefs?.edit()?.remove("ai_api_key")?.apply() }
    }
}

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val secureKeyStore: SecureKeyStore
) {
    val preferences: Flow<AppPreferences> = dataStore.data.map { prefs ->
        AppPreferences(
            onboardingCompleted = prefs[Keys.onboardingCompleted] ?: false,
            notificationsEnabled = prefs[Keys.notificationsEnabled] ?: true,
            reminderTime = prefs[Keys.reminderTime] ?: "18:00",
            themeMode = prefs[Keys.themeMode] ?: "system",
            aiMode = runCatching { AiMode.valueOf(prefs[Keys.aiMode] ?: AiMode.OFF.name) }.getOrDefault(AiMode.OFF),
            aiBaseUrl = prefs[Keys.aiBaseUrl] ?: "https://api.openai.com/v1",
            aiModel = prefs[Keys.aiModel] ?: "gpt-4o-mini",
            aiConsentAccepted = prefs[Keys.aiConsent] ?: false
        )
    }

    val onboardingCompleted: Flow<Boolean> = preferences.map { it.onboardingCompleted }

    val draft: Flow<OnboardingDraft?> = dataStore.data.map { prefs ->
        prefs[Keys.onboardingDraft]?.let { AppMoshi.readJson<OnboardingDraft>(it) }
    }

    suspend fun setOnboardingCompleted(value: Boolean) {
        dataStore.edit { it[Keys.onboardingCompleted] = value }
    }

    suspend fun saveDraft(draft: OnboardingDraft) {
        dataStore.edit { it[Keys.onboardingDraft] = AppMoshi.writeJson(draft) }
    }

    suspend fun clearDraft() {
        dataStore.edit { it.remove(Keys.onboardingDraft) }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        dataStore.edit { it[Keys.notificationsEnabled] = value }
    }

    suspend fun setReminderTime(value: String) {
        dataStore.edit { it[Keys.reminderTime] = value }
    }

    suspend fun setAiMode(value: AiMode) {
        dataStore.edit { it[Keys.aiMode] = value.name }
    }

    suspend fun setAiBaseUrl(value: String) {
        dataStore.edit { it[Keys.aiBaseUrl] = value }
    }

    suspend fun setAiModel(value: String) {
        dataStore.edit { it[Keys.aiModel] = value }
    }

    suspend fun setAiConsent(value: Boolean) {
        dataStore.edit { it[Keys.aiConsent] = value }
    }

    suspend fun aiSettings(): AiSettings {
        val prefs = preferences.first()
        return AiSettings(
            mode = prefs.aiMode,
            baseUrl = prefs.aiBaseUrl,
            model = prefs.aiModel,
            hasApiKey = secureKeyStore.hasApiKey(),
            consentAccepted = prefs.aiConsentAccepted
        )
    }

    fun hasApiKey(): Boolean = secureKeyStore.hasApiKey()
    fun readApiKey(): String? = secureKeyStore.readApiKey()
    fun saveApiKey(value: String) = secureKeyStore.saveApiKey(value)
    fun clearApiKey() = secureKeyStore.clearApiKey()
}
