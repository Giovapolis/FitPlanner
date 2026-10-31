package com.fitplanner.ai.feature.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitplanner.ai.core.data.repository.ExerciseRepository
import com.fitplanner.ai.core.model.Difficulty
import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.Exercise
import com.fitplanner.ai.core.model.LocationType
import com.fitplanner.ai.core.model.MuscleGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExercisesUiState(
    val all: List<Exercise> = emptyList(),
    val filtered: List<Exercise> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val query: String = "",
    val muscle: MuscleGroup? = null,
    val equipment: EquipmentType? = null,
    val location: LocationType? = null,
    val difficulty: Difficulty? = null,
    val favoritesOnly: Boolean = false,
    val loading: Boolean = true
)

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    private val repository: ExerciseRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ExercisesUiState())
    val state: StateFlow<ExercisesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.exercisesWithFavorites.collect { (exercises, favorites) ->
                _state.value = _state.value.copy(
                    all = exercises,
                    favorites = favorites,
                    loading = false
                )
                refresh()
            }
        }
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value)
        refresh()
    }

    fun setMuscle(value: MuscleGroup?) {
        _state.value = _state.value.copy(muscle = value)
        refresh()
    }

    fun setEquipment(value: EquipmentType?) {
        _state.value = _state.value.copy(equipment = value)
        refresh()
    }

    fun setLocation(value: LocationType?) {
        _state.value = _state.value.copy(location = value)
        refresh()
    }

    fun setDifficulty(value: Difficulty?) {
        _state.value = _state.value.copy(difficulty = value)
        refresh()
    }

    fun setFavoritesOnly(value: Boolean) {
        _state.value = _state.value.copy(favoritesOnly = value)
        refresh()
    }

    fun toggleFavorite(exercise: Exercise) {
        viewModelScope.launch {
            repository.toggleFavorite(exercise.id, exercise.id in _state.value.favorites)
        }
    }

    private fun refresh() {
        val current = _state.value
        _state.value = current.copy(
            filtered = repository.filter(
                exercises = current.all,
                query = current.query,
                muscle = current.muscle,
                equipment = current.equipment,
                location = current.location,
                difficulty = current.difficulty,
                favoritesOnly = current.favoritesOnly,
                favoriteIds = current.favorites
            )
        )
    }
}
