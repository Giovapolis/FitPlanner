package com.fitplanner.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitplanner.ai.core.data.repository.ExerciseRepository
import com.fitplanner.ai.core.data.settings.SettingsRepository
import com.fitplanner.ai.core.data.work.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    exerciseRepository: ExerciseRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    val onboardingCompleted: StateFlow<Boolean?> = settingsRepository.onboardingCompleted
        .map { it as Boolean? }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            runCatching { exerciseRepository.seedCatalog() }
        }
        viewModelScope.launch {
            settingsRepository.preferences.collect { preferences ->
                reminderScheduler.sync(preferences)
            }
        }
    }
}
