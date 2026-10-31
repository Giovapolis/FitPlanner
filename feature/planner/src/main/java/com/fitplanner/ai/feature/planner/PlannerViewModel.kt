package com.fitplanner.ai.feature.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitplanner.ai.core.data.repository.ExerciseRepository
import com.fitplanner.ai.core.data.repository.PlanRepository
import com.fitplanner.ai.core.data.repository.ProfileRepository
import com.fitplanner.ai.core.model.DayAvailability
import com.fitplanner.ai.core.model.EquipmentProfile
import com.fitplanner.ai.core.model.GenerationResult
import com.fitplanner.ai.core.model.HealthScreening
import com.fitplanner.ai.core.model.PlanGenerationInput
import com.fitplanner.ai.core.model.TrainingExperience
import com.fitplanner.ai.core.model.TrainingPlan
import com.fitplanner.ai.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class PlannerUiState(
    val plan: TrainingPlan? = null,
    val profile: UserProfile? = null,
    val health: HealthScreening? = null,
    val availability: List<DayAvailability> = emptyList(),
    val equipmentProfiles: List<EquipmentProfile> = emptyList(),
    val loading: Boolean = true,
    val generating: Boolean = false,
    val error: String? = null,
    val blockedTitle: String? = null,
    val blockedDetail: String? = null,
    val warnings: List<String> = emptyList(),
    val showConfirmGenerate: Boolean = false
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val exerciseRepository: ExerciseRepository,
    private val planRepository: PlanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PlannerUiState())
    val state: StateFlow<PlannerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.profile.collect { profile ->
                _state.value = _state.value.copy(profile = profile, loading = false)
            }
        }
        viewModelScope.launch {
            profileRepository.health.collect { health ->
                _state.value = _state.value.copy(health = health)
            }
        }
        viewModelScope.launch {
            profileRepository.availability.collect { availability ->
                _state.value = _state.value.copy(availability = availability)
            }
        }
        viewModelScope.launch {
            profileRepository.equipmentProfiles.collect { equipment ->
                _state.value = _state.value.copy(equipmentProfiles = equipment)
            }
        }
        viewModelScope.launch {
            planRepository.activePlan.collect { plan ->
                _state.value = _state.value.copy(plan = plan, loading = false)
            }
        }
    }

    fun requestGenerate() {
        _state.value = _state.value.copy(showConfirmGenerate = true)
    }

    fun dismissGenerate() {
        _state.value = _state.value.copy(showConfirmGenerate = false)
    }

    fun generate() {
        _state.value = _state.value.copy(
            showConfirmGenerate = false,
            generating = true,
            error = null,
            blockedTitle = null,
            blockedDetail = null,
            warnings = emptyList()
        )
        viewModelScope.launch {
            runCatching {
                val profile = profileRepository.profile.first()
                    ?: throw IllegalStateException("Completa tu perfil antes de generar un plan.")
                val health = profileRepository.health.first() ?: HealthScreening()
                val experience = profileRepository.experience.first() ?: TrainingExperience()
                var availability = profileRepository.availability.first()
                if (availability.isEmpty()) {
                    availability = DayOfWeek.values().map { DayAvailability(it) }
                }
                val equipment = profileRepository.equipmentProfiles.first().ifEmpty {
                    com.fitplanner.ai.core.model.OnboardingDraft().equipmentProfiles
                }
                var exercises = exerciseRepository.getAll()
                if (exercises.isEmpty()) {
                    exerciseRepository.seedCatalog()
                    exercises = exerciseRepository.getAll()
                }
                val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val input = PlanGenerationInput(
                    profile = profile,
                    health = health,
                    experience = experience,
                    availability = availability,
                    equipmentProfiles = equipment,
                    exercises = exercises,
                    weekStart = monday,
                    weekNumber = (_state.value.plan?.weeks?.maxOfOrNull { it.weekNumber } ?: 0) + 1
                )
                planRepository.generateAndSave(input)
            }.onSuccess { result ->
                when (result) {
                    is GenerationResult.Success -> _state.value = _state.value.copy(
                        generating = false,
                        warnings = result.warnings
                    )
                    is GenerationResult.Blocked -> _state.value = _state.value.copy(
                        generating = false,
                        blockedTitle = result.reason,
                        blockedDetail = result.detail
                    )
                    is GenerationResult.Error -> _state.value = _state.value.copy(
                        generating = false,
                        error = result.message
                    )
                }
            }.onFailure {
                _state.value = _state.value.copy(
                    generating = false,
                    error = it.message ?: "No se pudo generar el plan."
                )
            }
        }
    }
}
