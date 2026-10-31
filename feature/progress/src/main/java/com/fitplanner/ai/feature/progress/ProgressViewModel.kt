package com.fitplanner.ai.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitplanner.ai.core.data.repository.PlanRepository
import com.fitplanner.ai.core.domain.ProgressCalculator
import com.fitplanner.ai.core.model.Adjustment
import com.fitplanner.ai.core.model.PersonalRecord
import com.fitplanner.ai.core.model.TrainingPlan
import com.fitplanner.ai.core.model.TrainingWeek
import com.fitplanner.ai.core.model.WeekReview
import com.fitplanner.ai.core.model.WeeklyProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ProgressUiState(
    val plan: TrainingPlan? = null,
    val weekly: WeeklyProgress = WeeklyProgress(week = null),
    val records: List<PersonalRecord> = emptyList(),
    val history: List<TrainingWeek> = emptyList(),
    val loading: Boolean = true,
    val showReview: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val planRepository: PlanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProgressUiState())
    val state: StateFlow<ProgressUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            planRepository.activePlan.collect { plan ->
                val weekly = ProgressCalculator.weeklyProgress(plan, LocalDate.now())
                _state.value = _state.value.copy(
                    plan = plan,
                    weekly = weekly,
                    records = ProgressCalculator.personalRecords(plan),
                    history = plan?.weeks?.sortedByDescending { it.weekNumber }.orEmpty(),
                    loading = false
                )
            }
        }
    }

    fun openReview() {
        _state.value = _state.value.copy(showReview = true)
    }

    fun closeReview() {
        _state.value = _state.value.copy(showReview = false)
    }

    fun submitReview(
        completedSessions: Int,
        difficulty: Int,
        hadPain: Boolean,
        painNotes: String,
        metDuration: Boolean,
        adjustment: Adjustment
    ) {
        val plan = _state.value.plan ?: return
        val week = _state.value.weekly.week ?: return
        viewModelScope.launch {
            runCatching {
                planRepository.saveWeekReview(
                    plan = plan,
                    weekId = week.id,
                    review = WeekReview(
                        completedSessions = completedSessions,
                        perceivedDifficulty = difficulty,
                        hadPain = hadPain,
                        painNotes = painNotes,
                        metDuration = metDuration,
                        adjustment = if (hadPain) Adjustment.REDUCE else adjustment
                    )
                )
            }.onSuccess {
                _state.value = _state.value.copy(
                    showReview = false,
                    message = "Revisión guardada. Hemos creado la siguiente semana con un ajuste prudente."
                )
            }.onFailure {
                _state.value = _state.value.copy(
                    showReview = false,
                    error = it.message ?: "No se pudo guardar la revisión."
                )
            }
        }
    }
}
