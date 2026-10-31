package com.fitplanner.ai.core.domain

import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.Exercise
import com.fitplanner.ai.core.model.LocationType
import com.fitplanner.ai.core.model.PlanGenerationInput
import com.fitplanner.ai.core.model.SessionExercise

object SubstitutionEngine {
    fun alternatives(
        input: PlanGenerationInput,
        current: SessionExercise,
        sessionExerciseIds: Set<String>,
        location: LocationType
    ): List<Exercise> {
        val currentExercise = input.exercises.firstOrNull { it.id == current.exerciseId }
        val profile = input.equipmentProfiles.firstOrNull { it.location == location }
        val equipment = (profile?.items ?: emptySet()) + EquipmentType.BODYWEIGHT
        val restrictionTags = SafetyGate.restrictionTags(input.health)
        val avoid = (input.experience.exercisesToAvoid + input.health.avoidedExercises)
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        return input.exercises
            .filter { exercise ->
                exercise.id !in sessionExerciseIds &&
                    exercise.locations.contains(location) &&
                    exercise.equipment.all { it in equipment } &&
                    exercise.restrictionTags.intersect(restrictionTags).isEmpty() &&
                    exercise.difficulty.ordinal <= input.profile.experienceLevel.ordinal &&
                    avoid.none { term ->
                        exercise.nameEs.lowercase().contains(term) ||
                            exercise.id.lowercase().contains(term) ||
                            exercise.nameEn.lowercase().contains(term)
                    }
            }
            .sortedWith(
                compareByDescending<Exercise> {
                    (it.pattern == currentExercise?.pattern).compareTo(false)
                }.thenByDescending {
                    (it.primaryMuscle == current.primaryMuscle).compareTo(false)
                }.thenBy { it.difficulty.ordinal }.thenBy { it.id }
            )
            .take(12)
    }
}
