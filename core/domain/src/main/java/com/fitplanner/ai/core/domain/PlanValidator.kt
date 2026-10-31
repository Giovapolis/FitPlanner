package com.fitplanner.ai.core.domain

import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.MuscleGroup
import com.fitplanner.ai.core.model.PlanGenerationInput
import com.fitplanner.ai.core.model.TrainingPlan

data class ValidationReport(
    val fatal: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    val isValid: Boolean get() = fatal.isEmpty()
}

object PlanValidator {
    fun validate(plan: TrainingPlan, input: PlanGenerationInput): ValidationReport {
        val fatal = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val exercisesById = input.exercises.associateBy { it.id }
        val restrictionTags = SafetyGate.restrictionTags(input.health)
        val weeklySets = mutableMapOf<MuscleGroup, Int>()
        val allSessions = plan.weeks.flatMap { it.sessions }
        val trainingSessions = allSessions.filter { it.exercises.isNotEmpty() }

        if (trainingSessions.isEmpty()) {
            fatal += "El plan no contiene sesiones de entrenamiento."
        }
        trainingSessions.forEach { session ->
            if (session.exercises.isEmpty()) {
                fatal += "La sesión ${session.name} no tiene ejercicios."
            }
            val duplicates = session.exercises.groupBy { it.exerciseId }.filterValues { it.size > 1 }
            if (duplicates.isNotEmpty()) {
                fatal += "La sesión ${session.name} repite ejercicios."
            }
            val profile = input.equipmentProfiles.firstOrNull { it.location == session.location }
            val availableEquipment = (profile?.items ?: emptySet()) + EquipmentType.BODYWEIGHT
            session.exercises.forEach { sessionExercise ->
                val exercise = exercisesById[sessionExercise.exerciseId]
                if (exercise == null) {
                    fatal += "El ejercicio ${sessionExercise.exerciseName} no existe en el catálogo."
                    return@forEach
                }
                if (!exercise.equipment.all { it in availableEquipment }) {
                    fatal += "${exercise.nameEs} requiere equipamiento no disponible."
                }
                if (exercise.restrictionTags.intersect(restrictionTags).isNotEmpty()) {
                    fatal += "${exercise.nameEs} entra en conflicto con una restricción declarada."
                }
                weeklySets[exercise.primaryMuscle] =
                    (weeklySets[exercise.primaryMuscle] ?: 0) + sessionExercise.sets
            }
            val day = input.availability.firstOrNull { it.dayOfWeek == session.date.dayOfWeek }
            val maxMinutes = day?.maxMinutes ?: 90
            if (session.estimatedMinutes > maxMinutes + 10) {
                warnings += "La sesión ${session.name} supera en más de 10 minutos el tiempo disponible."
            }
        }
        val highVolume = weeklySets.filterValues { it > 20 }
        if (highVolume.isNotEmpty()) {
            warnings += "El volumen semanal de ${highVolume.keys.joinToString { it.label }} es alto; revisa la recuperación."
        }
        val sortedDates = trainingSessions.map { it.date }.distinct().sorted()
        var consecutive = 1
        for (i in 1 until sortedDates.size) {
            if (sortedDates[i] == sortedDates[i - 1].plusDays(1)) {
                consecutive++
                if (consecutive > 3) {
                    warnings += "Hay más de 3 días consecutivos de entrenamiento; considera añadir descanso."
                    break
                }
            } else {
                consecutive = 1
            }
        }
        return ValidationReport(fatal.distinct(), warnings.distinct())
    }

    fun validateSuggestion(
        slugs: List<String>,
        input: PlanGenerationInput,
        sessionLocation: com.fitplanner.ai.core.model.LocationType,
        currentExerciseIds: Set<String>
    ): List<String> {
        val byId = input.exercises.associateBy { it.id }
        val profile = input.equipmentProfiles.firstOrNull { it.location == sessionLocation }
        val equipment = (profile?.items ?: emptySet()) + EquipmentType.BODYWEIGHT
        val restrictionTags = SafetyGate.restrictionTags(input.health)
        return slugs.filter { slug ->
            val exercise = byId[slug]
            exercise != null &&
                exercise.locations.contains(sessionLocation) &&
                exercise.equipment.all { it in equipment } &&
                exercise.restrictionTags.intersect(restrictionTags).isEmpty() &&
                slug !in currentExerciseIds &&
                exercise.difficulty.ordinal <= input.profile.experienceLevel.ordinal
        }
    }
}
