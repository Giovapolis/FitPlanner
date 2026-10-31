package com.fitplanner.ai.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitplanner.ai.core.data.repository.ProfileRepository
import com.fitplanner.ai.core.data.settings.SettingsRepository
import com.fitplanner.ai.core.domain.SafetyGate
import com.fitplanner.ai.core.domain.SafetyGateResult
import com.fitplanner.ai.core.model.AgeRange
import com.fitplanner.ai.core.model.ActivityLevel
import com.fitplanner.ai.core.model.BodyArea
import com.fitplanner.ai.core.model.DayAvailability
import com.fitplanner.ai.core.model.EquipmentProfile
import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.ExperienceLevel
import com.fitplanner.ai.core.model.Goal
import com.fitplanner.ai.core.model.HealthScreening
import com.fitplanner.ai.core.model.IntensityPreference
import com.fitplanner.ai.core.model.LocationType
import com.fitplanner.ai.core.model.MuscleGroup
import com.fitplanner.ai.core.model.OnboardingDraft
import com.fitplanner.ai.core.model.SessionType
import com.fitplanner.ai.core.model.TrainingExperience
import com.fitplanner.ai.core.model.UnitSystem
import com.fitplanner.ai.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SafetyWarning(
    val reason: String,
    val detail: String
)

data class OnboardingUiState(
    val draft: OnboardingDraft = OnboardingDraft(),
    val step: Int = 0,
    val totalSteps: Int = 5,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val safetyWarning: SafetyWarning? = null,
    val completed: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val savedDraft = settingsRepository.draft.first()
            val draft = savedDraft ?: profileRepository.loadOnboardingSnapshot()
            _state.value = _state.value.copy(draft = draft, loading = false)
        }
    }

    fun updateProfile(transform: (UserProfile) -> UserProfile) {
        updateDraft { it.copy(profile = transform(it.profile)) }
    }

    fun updateHealth(transform: (HealthScreening) -> HealthScreening) {
        updateDraft { it.copy(health = transform(it.health)) }
    }

    fun updateExperience(transform: (TrainingExperience) -> TrainingExperience) {
        updateDraft { it.copy(experience = transform(it.experience)) }
    }

    fun updateAvailability(index: Int, transform: (DayAvailability) -> DayAvailability) {
        updateDraft { draft ->
            draft.copy(
                availability = draft.availability.mapIndexed { i, day ->
                    if (i == index) transform(day) else day
                }
            )
        }
    }

    fun updateEquipment(location: LocationType, transform: (Set<EquipmentType>) -> Set<EquipmentType>) {
        updateDraft { draft ->
            draft.copy(
                equipmentProfiles = draft.equipmentProfiles.map { profile ->
                    if (profile.location == location) profile.copy(items = transform(profile.items)) else profile
                }
            )
        }
    }

    fun toggleEquipment(location: LocationType, equipment: EquipmentType) {
        updateEquipment(location) { items ->
            if (equipment in items) items - equipment else items + equipment
        }
    }

    fun setDisclaimerAccepted(value: Boolean) {
        updateDraft { it.copy(disclaimerAccepted = value) }
    }

    fun next() {
        val current = _state.value
        if (current.step >= current.totalSteps - 1) return
        if (current.step == 3 && current.draft.availability.none { it.available && it.maxMinutes >= 15 }) {
            _state.value = current.copy(error = "Selecciona al menos un día con 15 minutos o más.")
            return
        }
        viewModelScope.launch {
            settingsRepository.saveDraft(current.draft.copy(step = current.step + 1))
            _state.value = current.copy(step = current.step + 1, error = null)
        }
    }

    fun back() {
        val current = _state.value
        if (current.step == 0) return
        viewModelScope.launch {
            settingsRepository.saveDraft(current.draft.copy(step = current.step - 1))
            _state.value = current.copy(step = current.step - 1, error = null)
        }
    }

    fun finish() {
        val current = _state.value
        if (!current.draft.disclaimerAccepted) {
            _state.value = current.copy(error = "Acepta el aviso educativo para continuar.")
            return
        }
        when (val safety = SafetyGate.evaluate(current.draft.health)) {
            is SafetyGateResult.Blocked -> {
                _state.value = current.copy(
                    safetyWarning = SafetyWarning(safety.reason, safety.detail),
                    error = null
                )
            }
            is SafetyGateResult.Allowed -> completeOnboarding(current.draft)
        }
    }

    fun confirmWarningAndFinish() {
        completeOnboarding(_state.value.draft)
    }

    fun dismissWarning() {
        _state.value = _state.value.copy(safetyWarning = null)
    }

    private fun completeOnboarding(draft: OnboardingDraft) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            runCatching {
                profileRepository.saveOnboarding(draft)
                settingsRepository.setOnboardingCompleted(true)
                settingsRepository.clearDraft()
            }.onSuccess {
                _state.value = _state.value.copy(saving = false, completed = true, safetyWarning = null)
            }.onFailure {
                _state.value = _state.value.copy(
                    saving = false,
                    error = it.message ?: "No se pudo guardar el perfil."
                )
            }
        }
    }

    private fun updateDraft(transform: (OnboardingDraft) -> OnboardingDraft) {
        _state.value = _state.value.copy(draft = transform(_state.value.draft), error = null)
    }

    companion object {
        val goals = Goal.values().toList()
        val experienceLevels = ExperienceLevel.values().toList()
        val activityLevels = ActivityLevel.values().toList()
        val ageRanges = AgeRange.values().toList()
        val unitSystems = UnitSystem.values().toList()
        val bodyAreas = BodyArea.values().toList()
        val intensities = IntensityPreference.values().toList()
        val sessionTypes = SessionType.values().toList()
        val muscles = MuscleGroup.values().toList()
    }
}
