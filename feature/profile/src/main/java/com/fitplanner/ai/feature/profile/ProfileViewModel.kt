package com.fitplanner.ai.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitplanner.ai.core.data.repository.PlanRepository
import com.fitplanner.ai.core.data.repository.ProfileRepository
import com.fitplanner.ai.core.data.settings.SettingsRepository
import com.fitplanner.ai.core.model.AiMode
import com.fitplanner.ai.core.model.AiSettings
import com.fitplanner.ai.core.model.AppPreferences
import com.fitplanner.ai.core.model.DayAvailability
import com.fitplanner.ai.core.model.EquipmentProfile
import com.fitplanner.ai.core.model.HealthScreening
import com.fitplanner.ai.core.model.TrainingExperience
import com.fitplanner.ai.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val health: HealthScreening? = null,
    val experience: TrainingExperience? = null,
    val availability: List<DayAvailability> = emptyList(),
    val equipmentProfiles: List<EquipmentProfile> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val aiSettings: AiSettings = AiSettings(),
    val loading: Boolean = true,
    val showResetConfirm: Boolean = false,
    val showClearHistoryConfirm: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val planRepository: PlanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.profile.collect { _state.value = _state.value.copy(profile = it) }
        }
        viewModelScope.launch {
            profileRepository.health.collect { _state.value = _state.value.copy(health = it) }
        }
        viewModelScope.launch {
            profileRepository.experience.collect { _state.value = _state.value.copy(experience = it) }
        }
        viewModelScope.launch {
            profileRepository.availability.collect { _state.value = _state.value.copy(availability = it) }
        }
        viewModelScope.launch {
            profileRepository.equipmentProfiles.collect {
                _state.value = _state.value.copy(equipmentProfiles = it, loading = false)
            }
        }
        viewModelScope.launch {
            settingsRepository.preferences.collect {
                _state.value = _state.value.copy(preferences = it)
            }
        }
        viewModelScope.launch {
            val ai = settingsRepository.aiSettings()
            _state.value = _state.value.copy(aiSettings = ai)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun setReminderTime(value: String) {
        viewModelScope.launch { settingsRepository.setReminderTime(value) }
    }

    fun saveAiSettings(
        mode: AiMode,
        baseUrl: String,
        model: String,
        apiKey: String,
        consent: Boolean
    ) {
        viewModelScope.launch {
            settingsRepository.setAiMode(mode)
            settingsRepository.setAiBaseUrl(baseUrl.trim())
            settingsRepository.setAiModel(model.trim())
            settingsRepository.setAiConsent(consent)
            if (apiKey.isNotBlank()) settingsRepository.saveApiKey(apiKey.trim())
            val ai = settingsRepository.aiSettings()
            _state.value = _state.value.copy(
                aiSettings = ai,
                message = "Configuración de IA guardada. La validación local sigue siendo obligatoria."
            )
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            settingsRepository.clearApiKey()
            _state.value = _state.value.copy(
                aiSettings = settingsRepository.aiSettings(),
                message = "Clave de IA eliminada del dispositivo."
            )
        }
    }

    fun requestReset() {
        _state.value = _state.value.copy(showResetConfirm = true)
    }

    fun dismissReset() {
        _state.value = _state.value.copy(showResetConfirm = false)
    }

    fun confirmReset() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(false)
            settingsRepository.clearDraft()
            _state.value = _state.value.copy(
                showResetConfirm = false,
                message = "Asistente reiniciado. Tus datos siguen precargados para que puedas revisarlos."
            )
        }
    }

    fun requestClearHistory() {
        _state.value = _state.value.copy(showClearHistoryConfirm = true)
    }

    fun dismissClearHistory() {
        _state.value = _state.value.copy(showClearHistoryConfirm = false)
    }

    fun confirmClearHistory() {
        viewModelScope.launch {
            planRepository.clearHistory()
            _state.value = _state.value.copy(
                showClearHistoryConfirm = false,
                message = "Historial de planes eliminado."
            )
        }
    }
}
