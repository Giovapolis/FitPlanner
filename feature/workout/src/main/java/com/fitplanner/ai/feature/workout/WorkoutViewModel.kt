package com.fitplanner.ai.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitplanner.ai.core.data.ai.AiRepository
import com.fitplanner.ai.core.data.repository.ExerciseRepository
import com.fitplanner.ai.core.data.repository.PlanRepository
import com.fitplanner.ai.core.data.repository.ProfileRepository
import com.fitplanner.ai.core.domain.SubstitutionEngine
import com.fitplanner.ai.core.model.Exercise
import com.fitplanner.ai.core.model.ExerciseStatus
import com.fitplanner.ai.core.model.HealthScreening
import com.fitplanner.ai.core.model.PlanGenerationInput
import com.fitplanner.ai.core.model.SessionExercise
import com.fitplanner.ai.core.model.SessionStatus
import com.fitplanner.ai.core.model.TrainingExperience
import com.fitplanner.ai.core.model.WorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class WorkoutUiState(
    val session: WorkoutSession? = null,
    val currentExerciseIndex: Int = 0,
    val restRemaining: Int = 0,
    val restRunning: Boolean = false,
    val alternatives: List<Exercise> = emptyList(),
    val showAlternatives: Boolean = false,
    val aiLoading: Boolean = false,
    val aiMessage: String? = null,
    val exerciseDetails: Map<String, Exercise> = emptyMap(),
    val loading: Boolean = true,
    val error: String? = null,
    val finished: Boolean = false
) {
    val currentExercise: SessionExercise?
        get() = session?.exercises?.getOrNull(currentExerciseIndex)
}

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val planRepository: PlanRepository,
    private val exerciseRepository: ExerciseRepository,
    private val profileRepository: ProfileRepository,
    private val aiRepository: AiRepository
) : ViewModel() {
    private val sessionId: Long = savedStateHandle["sessionId"] ?: 0L
    private val _state = MutableStateFlow(WorkoutUiState())
    val state: StateFlow<WorkoutUiState> = _state.asStateFlow()
    private var startRequested = false
    private var restJob: Job? = null

    init {
        viewModelScope.launch {
            exerciseRepository.exercises.collect { exercises ->
                _state.value = _state.value.copy(exerciseDetails = exercises.associateBy { it.id })
            }
        }
        viewModelScope.launch {
            planRepository.activePlan.collect { plan ->
                val session = plan?.weeks
                    ?.flatMap { it.sessions }
                    ?.firstOrNull { it.id == sessionId }
                _state.value = _state.value.copy(
                    session = session,
                    loading = false,
                    error = if (session == null) "Sesión no encontrada" else null
                )
                if (plan != null && session != null && session.status == SessionStatus.PLANNED && !startRequested) {
                    startRequested = true
                    planRepository.startSession(plan, sessionId)
                }
            }
        }
    }

    fun currentExercise() = _state.value.currentExercise

    fun logSet(reps: Int, weightKg: Double?, rpe: Int?) {
        val current = _state.value.currentExercise ?: return
        if (reps <= 0 && current.timeMinutes == null) return
        val session = _state.value.session ?: return
        viewModelScope.launch {
            val plan = planRepository.getActivePlan() ?: return@launch
            val updated = planRepository.logSet(
                plan = plan,
                sessionId = session.id,
                sessionExerciseId = current.id,
                reps = reps,
                weightKg = weightKg,
                rpe = rpe
            )
            val updatedSession = updated.weeks.flatMap { it.sessions }.firstOrNull { it.id == session.id }
            val updatedExercise = updatedSession?.exercises?.firstOrNull { it.id == current.id }
            startRest(current.restSeconds)
            if (updatedExercise != null && updatedExercise.logs.size >= updatedExercise.sets) {
                val nextIndex = updatedSession?.exercises?.indexOfFirst {
                    it.status != ExerciseStatus.COMPLETED && it.status != ExerciseStatus.SKIPPED
                } ?: -1
                if (nextIndex >= 0) {
                    _state.value = _state.value.copy(currentExerciseIndex = nextIndex)
                }
            }
        }
    }

    fun nextExercise() {
        val session = _state.value.session ?: return
        if (session.exercises.isEmpty()) return
        _state.value = _state.value.copy(
            currentExerciseIndex = (_state.value.currentExerciseIndex + 1).coerceAtMost(session.exercises.lastIndex)
        )
    }

    fun previousExercise() {
        _state.value = _state.value.copy(
            currentExerciseIndex = (_state.value.currentExerciseIndex - 1).coerceAtLeast(0)
        )
    }

    fun skipExercise() {
        val current = _state.value.currentExercise ?: return
        val session = _state.value.session ?: return
        viewModelScope.launch {
            val plan = planRepository.getActivePlan() ?: return@launch
            planRepository.skipExercise(plan, session.id, current.id)
            nextExercise()
        }
    }

    fun startRest(seconds: Int) {
        restJob?.cancel()
        if (seconds <= 0) {
            _state.value = _state.value.copy(restRemaining = 0, restRunning = false)
            return
        }
        _state.value = _state.value.copy(restRemaining = seconds, restRunning = true)
        restJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _state.value = _state.value.copy(restRemaining = remaining)
            }
            _state.value = _state.value.copy(restRunning = false)
        }
    }

    fun addRest(seconds: Int = 15) {
        if (_state.value.restRemaining == 0) {
            startRest(seconds)
        } else {
            _state.value = _state.value.copy(restRemaining = _state.value.restRemaining + seconds)
        }
    }

    fun skipRest() {
        restJob?.cancel()
        _state.value = _state.value.copy(restRemaining = 0, restRunning = false)
    }

    fun finish() {
        val session = _state.value.session ?: return
        restJob?.cancel()
        viewModelScope.launch {
            val plan = planRepository.getActivePlan() ?: return@launch
            planRepository.finishSession(plan, session.id)
            _state.value = _state.value.copy(finished = true, restRunning = false, restRemaining = 0)
        }
    }

    fun openAlternatives() {
        val current = _state.value.currentExercise ?: return
        val session = _state.value.session ?: return
        viewModelScope.launch {
            val profile = profileRepository.profile.first() ?: return@launch
            val health = profileRepository.health.first() ?: HealthScreening()
            val experience = profileRepository.experience.first() ?: TrainingExperience()
            val availability = profileRepository.availability.first()
            val equipment = profileRepository.equipmentProfiles.first()
            val exercises = exerciseRepository.getAll()
            val input = PlanGenerationInput(
                profile = profile,
                health = health,
                experience = experience,
                availability = availability,
                equipmentProfiles = equipment,
                exercises = exercises,
                weekStart = LocalDate.now(),
                weekNumber = 1
            )
            val alternatives = SubstitutionEngine.alternatives(
                input = input,
                current = current,
                sessionExerciseIds = session.exercises.map { it.exerciseId }.toSet(),
                location = session.location
            )
            _state.value = _state.value.copy(
                alternatives = alternatives,
                showAlternatives = true,
                aiMessage = null
            )
        }
    }

    fun requestAiAlternatives() {
        val current = _state.value.currentExercise ?: return
        val session = _state.value.session ?: return
        if (_state.value.aiLoading) return
        _state.value = _state.value.copy(aiLoading = true, aiMessage = null)
        viewModelScope.launch {
            aiRepository.suggestAlternatives(current, session)
                .onSuccess { result ->
                    val merged = (result.exercises + _state.value.alternatives)
                        .distinctBy { it.id }
                    val message = when {
                        result.exercises.isEmpty() && result.discardedCount > 0 ->
                            "La IA propuso alternativas que no cumplían tus restricciones o equipo. Se descartaron."
                        result.exercises.isEmpty() ->
                            "La IA no propuso alternativas compatibles. Puedes seguir con las locales."
                        result.discardedCount > 0 ->
                            "Sugerencias validadas. ${result.discardedCount} propuesta(s) se descartaron por seguridad o equipo."
                        else -> "Sugerencias validadas por el motor local. Revísalas antes de aplicar."
                    }
                    _state.value = _state.value.copy(
                        alternatives = merged,
                        aiLoading = false,
                        aiMessage = message
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        aiLoading = false,
                        aiMessage = error.message ?: "No se pudo consultar la IA."
                    )
                }
        }
    }

    fun closeAlternatives() {
        _state.value = _state.value.copy(
            showAlternatives = false,
            alternatives = emptyList(),
            aiMessage = null,
            aiLoading = false
        )
    }

    fun substitute(exercise: Exercise) {
        val current = _state.value.currentExercise ?: return
        val session = _state.value.session ?: return
        viewModelScope.launch {
            val plan = planRepository.getActivePlan() ?: return@launch
            planRepository.substituteExercise(plan, session.id, current.id, exercise)
            _state.value = _state.value.copy(
                showAlternatives = false,
                alternatives = emptyList(),
                aiMessage = null
            )
        }
    }

    override fun onCleared() {
        restJob?.cancel()
        super.onCleared()
    }
}
