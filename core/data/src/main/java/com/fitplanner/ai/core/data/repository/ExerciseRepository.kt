package com.fitplanner.ai.core.data.repository

import com.fitplanner.ai.core.data.AppMoshi
import com.fitplanner.ai.core.data.db.ExerciseDao
import com.fitplanner.ai.core.data.db.FavoriteEntity
import com.fitplanner.ai.core.data.seed.ExerciseSeed
import com.fitplanner.ai.core.data.toDomain
import com.fitplanner.ai.core.data.toEntity
import com.fitplanner.ai.core.model.Difficulty
import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.Exercise
import com.fitplanner.ai.core.model.LocationType
import com.fitplanner.ai.core.model.MuscleGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val dao: ExerciseDao
) {
    val exercises: Flow<List<Exercise>> = dao.observeAll()
        .map { list -> list.mapNotNull { it.toDomain(AppMoshi) } }

    val favorites: Flow<Set<String>> = dao.observeFavorites()
        .map { it.toSet() }

    val exercisesWithFavorites: Flow<Pair<List<Exercise>, Set<String>>> =
        combine(exercises, favorites) { list, favoriteIds -> list to favoriteIds }

    suspend fun seedCatalog() {
        dao.upsertAll(ExerciseSeed.all.map { it.toEntity(AppMoshi) })
    }

    suspend fun getAll(): List<Exercise> =
        dao.getAll().mapNotNull { it.toDomain(AppMoshi) }

    suspend fun getById(id: String): Exercise? = dao.getById(id)?.toDomain(AppMoshi)

    suspend fun toggleFavorite(id: String, currentlyFavorite: Boolean) {
        if (currentlyFavorite) dao.removeFavorite(id) else dao.addFavorite(FavoriteEntity(id))
    }

    fun filter(
        exercises: List<Exercise>,
        query: String,
        muscle: MuscleGroup?,
        equipment: EquipmentType?,
        location: LocationType?,
        difficulty: Difficulty?,
        favoritesOnly: Boolean,
        favoriteIds: Set<String>
    ): List<Exercise> {
        val normalized = query.trim().lowercase()
        return exercises.filter { exercise ->
            (normalized.isEmpty() ||
                exercise.nameEs.lowercase().contains(normalized) ||
                exercise.nameEn.lowercase().contains(normalized) ||
                exercise.primaryMuscle.label.lowercase().contains(normalized)) &&
                (muscle == null || exercise.primaryMuscle == muscle) &&
                (equipment == null || equipment in exercise.equipment) &&
                (location == null || location in exercise.locations) &&
                (difficulty == null || exercise.difficulty == difficulty) &&
                (!favoritesOnly || exercise.id in favoriteIds)
        }
    }
}
