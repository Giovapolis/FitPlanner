package com.fitplanner.ai.feature.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitplanner.ai.core.data.repository.ExerciseRepository
import com.fitplanner.ai.core.model.Exercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val isFavorite: Boolean = false,
    val loading: Boolean = true
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository
) : ViewModel() {
    private val exerciseId: String = savedStateHandle["exerciseId"] ?: ""
    private val _state = MutableStateFlow(ExerciseDetailUiState())
    val state: StateFlow<ExerciseDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.exercisesWithFavorites.collect { (exercises, favorites) ->
                val exercise = exercises.firstOrNull { it.id == exerciseId }
                _state.value = ExerciseDetailUiState(
                    exercise = exercise,
                    isFavorite = exerciseId in favorites,
                    loading = false
                )
            }
        }
    }

    fun toggleFavorite() {
        val current = _state.value.exercise ?: return
        viewModelScope.launch {
            repository.toggleFavorite(current.id, _state.value.isFavorite)
        }
    }
}
