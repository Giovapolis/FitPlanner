package com.fitplanner.ai.core.domain

import com.fitplanner.ai.core.model.DayAvailability
import com.fitplanner.ai.core.model.Difficulty
import com.fitplanner.ai.core.model.EquipmentProfile
import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.Exercise
import com.fitplanner.ai.core.model.ExerciseStatus
import com.fitplanner.ai.core.model.ExerciseType
import com.fitplanner.ai.core.model.ExperienceLevel
import com.fitplanner.ai.core.model.GenerationResult
import com.fitplanner.ai.core.model.Goal
import com.fitplanner.ai.core.model.LocationType
import com.fitplanner.ai.core.model.MovementPattern
import com.fitplanner.ai.core.model.PlanGenerationInput
import com.fitplanner.ai.core.model.PlanStatus
import com.fitplanner.ai.core.model.SessionExercise
import com.fitplanner.ai.core.model.SessionStatus
import com.fitplanner.ai.core.model.SessionType
import com.fitplanner.ai.core.model.TrainingPlan
import com.fitplanner.ai.core.model.TrainingWeek
import com.fitplanner.ai.core.model.WeekStatus
import com.fitplanner.ai.core.model.WorkoutSession
import kotlin.math.abs

internal data class SessionBlueprint(
    val name: String,
    val objective: String,
    val patterns: List<MovementPattern>,
    val type: SessionType
)

private data class RepScheme(
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val restSeconds: Int,
    val timeMinutes: Int? = null
)

internal object SplitSelector {
    fun select(
        days: Int,
        level: ExperienceLevel,
        goal: Goal
    ): List<SessionBlueprint> {
        if (goal == Goal.MOBILITY) {
            return List(days.coerceIn(1, 4)) { index ->
                SessionBlueprint(
                    name = listOf("Movilidad y control", "Movilidad de cadera", "Movilidad torácica", "Recuperación activa")[index % 4],
                    objective = "Movilidad, control y respiración",
                    patterns = listOf(MovementPattern.MOBILITY, MovementPattern.CORE, MovementPattern.HINGE),
                    type = SessionType.MOBILITY
                )
            }
        }
        val fullBody = listOf(
            SessionBlueprint("Cuerpo completo A", "Fuerza general y técnica", listOf(MovementPattern.SQUAT, MovementPattern.HORIZONTAL_PUSH, MovementPattern.HORIZONTAL_PULL, MovementPattern.CORE), SessionType.FULL_BODY),
            SessionBlueprint("Cuerpo completo B", "Bisagra, tracción y estabilidad", listOf(MovementPattern.HINGE, MovementPattern.VERTICAL_PUSH, MovementPattern.VERTICAL_PULL, MovementPattern.LUNGE), SessionType.FULL_BODY),
            SessionBlueprint("Cuerpo completo C", "Patrones complementarios", listOf(MovementPattern.LUNGE, MovementPattern.HORIZONTAL_PUSH, MovementPattern.HORIZONTAL_PULL, MovementPattern.CORE), SessionType.FULL_BODY)
        )
        val push = SessionBlueprint("Empuje", "Pecho, hombros y tríceps", listOf(MovementPattern.HORIZONTAL_PUSH, MovementPattern.VERTICAL_PUSH, MovementPattern.CORE), SessionType.UPPER)
        val pull = SessionBlueprint("Tirón", "Espalda y bíceps", listOf(MovementPattern.HORIZONTAL_PULL, MovementPattern.VERTICAL_PULL, MovementPattern.CORE), SessionType.UPPER)
        val legs = SessionBlueprint("Piernas", "Piernas y cadera", listOf(MovementPattern.SQUAT, MovementPattern.HINGE, MovementPattern.LUNGE), SessionType.LOWER)
        val upper = SessionBlueprint("Tren superior", "Empuje y tracción", listOf(MovementPattern.HORIZONTAL_PUSH, MovementPattern.HORIZONTAL_PULL, MovementPattern.VERTICAL_PUSH, MovementPattern.VERTICAL_PULL), SessionType.UPPER)
        val lower = SessionBlueprint("Tren inferior", "Piernas, cadera y core", listOf(MovementPattern.SQUAT, MovementPattern.HINGE, MovementPattern.LUNGE, MovementPattern.CORE), SessionType.LOWER)
        val cardio = SessionBlueprint("Acondicionamiento", "Cardio y resistencia", listOf(MovementPattern.CARDIO, MovementPattern.LUNGE, MovementPattern.CORE), SessionType.CARDIO)

        return when (days.coerceIn(1, 6)) {
            1 -> listOf(fullBody[0])
            2 -> listOf(fullBody[0], fullBody[1])
            3 -> if (level == ExperienceLevel.BEGINNER) fullBody else listOf(push, pull, legs)
            4 -> listOf(upper, lower, upper, lower)
            5 -> if (goal == Goal.CARDIO || goal == Goal.FAT_LOSS) listOf(push, pull, legs, upper, cardio)
            else listOf(push, pull, legs, upper, lower)
            else -> listOf(push, pull, legs, push, pull, legs)
        }
    }
}

object PlanGenerator {
    fun generate(input: PlanGenerationInput): GenerationResult {
        val safety = SafetyGate.evaluate(input.health)
        if (safety is SafetyGateResult.Blocked) {
            return GenerationResult.Blocked(safety.reason, safety.detail)
        }
        val availableDays = input.availability
            .filter { it.available && it.maxMinutes >= 15 }
            .sortedBy { it.dayOfWeek.value }
        if (availableDays.isEmpty()) {
            return GenerationResult.Blocked(
                "No hay días disponibles",
                "Selecciona al menos un día con 15 minutos o más para generar un plan."
            )
        }
        if (input.equipmentProfiles.isEmpty()) {
            return GenerationResult.Error("No hay perfiles de equipamiento configurados.")
        }

        val blueprints = SplitSelector.select(
            days = minOf(availableDays.size, 6),
            level = input.profile.experienceLevel,
            goal = input.profile.primaryGoal
        )
        val weeklyUse = mutableMapOf<String, Int>()
        val sessions = mutableListOf<WorkoutSession>()
        val warnings = (safety as SafetyGateResult.Allowed).warnings.toMutableList()
        var sessionIndex = 0
        availableDays.forEachIndexed { index, day ->
            if (index >= 6) {
                sessions += recoverySession(input, day, index)
                return@forEachIndexed
            }
            val location = locationFor(index, input.equipmentProfiles)
            val profile = input.equipmentProfiles.firstOrNull { it.location == location }
                ?: input.equipmentProfiles.first()
            val blueprint = blueprints[index % blueprints.size]
            val session = buildSession(
                input = input,
                day = day,
                blueprint = blueprint,
                profile = profile,
                location = location,
                weeklyUse = weeklyUse,
                index = sessionIndex++
            )
            if (session == null) {
                warnings += "No encontramos ejercicios compatibles para ${day.dayOfWeek.name.lowercase()}; ese día queda como recuperación."
                sessions += recoverySession(input, day, index)
            } else {
                sessions += session
            }
        }

        sessions.filter { it.exercises.size == 1 }.forEach { session ->
            warnings += "${session.name} se redujo a un ejercicio para respetar el tiempo disponible."
        }
        if (sessions.none { it.exercises.isNotEmpty() }) {
            return GenerationResult.Blocked(
                "No hay ejercicios compatibles",
                "Con el equipamiento y las restricciones indicadas no encontramos una sesión segura. Revisa tu equipamiento o consulta a un profesional."
            )
        }

        val week = TrainingWeek(
            id = System.currentTimeMillis(),
            weekNumber = input.weekNumber,
            startDate = input.weekStart,
            endDate = input.weekStart.plusDays(6),
            focus = blueprints.firstOrNull()?.name ?: "Plan personalizado",
            status = WeekStatus.ACTIVE,
            sessions = sessions
        )
        val plan = TrainingPlan(
            id = System.currentTimeMillis() + 1,
            name = "Plan de ${input.profile.primaryGoal.label.lowercase()}",
            status = PlanStatus.ACTIVE,
            weeks = listOf(week)
        )
        val report = PlanValidator.validate(plan, input)
        if (report.fatal.isNotEmpty()) {
            return GenerationResult.Blocked(
                "El plan no cumple las restricciones",
                report.fatal.joinToString(" ")
            )
        }
        warnings += report.warnings
        return GenerationResult.Success(plan, warnings.distinct())
    }

    private fun locationFor(index: Int, profiles: List<EquipmentProfile>): LocationType {
        val hasHome = profiles.any { it.location == LocationType.HOME }
        val hasGym = profiles.any { it.location == LocationType.GYM }
        if (hasHome && hasGym) {
            return if (index % 2 == 0) LocationType.HOME else LocationType.GYM
        }
        return if (hasHome) LocationType.HOME else LocationType.GYM
    }

    private fun buildSession(
        input: PlanGenerationInput,
        day: DayAvailability,
        blueprint: SessionBlueprint,
        profile: EquipmentProfile,
        location: LocationType,
        weeklyUse: MutableMap<String, Int>,
        index: Int
    ): WorkoutSession? {
        val maxMinutes = day.maxMinutes.coerceIn(15, 120)
        val candidates = filterCandidates(input, profile, location)
        if (candidates.isEmpty()) return null

        val selected = mutableListOf<Exercise>()
        blueprint.patterns.forEach { pattern ->
            val candidate = candidates
                .filter { it.pattern == pattern && (weeklyUse[it.id] ?: 0) < 2 && selected.none { s -> s.id == it.id } }
                .sortedWith(candidateComparator(input))
                .firstOrNull()
            if (candidate != null) selected += candidate
        }
        val targetCount = targetExerciseCount(maxMinutes, input.profile.primaryGoal)
        val fillers = candidates
            .filter { (weeklyUse[it.id] ?: 0) < 2 && selected.none { s -> s.id == it.id } }
            .sortedWith(candidateComparator(input))
        for (candidate in fillers) {
            if (selected.size >= targetCount) break
            selected += candidate
        }
        if (selected.isEmpty()) return null

        var exercises = selected.mapIndexed { exerciseIndex, exercise ->
            val scheme = schemeFor(input.profile.primaryGoal, input.profile.experienceLevel, exercise, maxMinutes)
            SessionExercise(
                id = System.currentTimeMillis() + index * 100L + exerciseIndex,
                exerciseId = exercise.id,
                exerciseName = exercise.nameEs,
                primaryMuscle = exercise.primaryMuscle,
                orderIndex = exerciseIndex,
                sets = scheme.sets,
                repsMin = scheme.repsMin,
                repsMax = scheme.repsMax,
                restSeconds = scheme.restSeconds,
                timeMinutes = scheme.timeMinutes,
                intensityNote = intensityNote(input.profile.primaryGoal, input.profile.experienceLevel),
                techniqueCue = exercise.keyPoints.firstOrNull() ?: exercise.summary,
                status = ExerciseStatus.PENDING
            )
        }

        val warmupMinutes = DurationEstimator.warmupMinutes(maxMinutes)
        val cooldownMinutes = DurationEstimator.cooldownMinutes(maxMinutes)
        var estimate = DurationEstimator.estimateExercises(exercises, warmupMinutes, cooldownMinutes)
        while (estimate > maxMinutes && exercises.size > 1) {
            exercises = exercises.dropLast(1).mapIndexed { i, item -> item.copy(orderIndex = i) }
            estimate = DurationEstimator.estimateExercises(exercises, warmupMinutes, cooldownMinutes)
        }
        while (estimate > maxMinutes && exercises.any { it.sets > 2 }) {
            exercises = exercises.map { if (it.sets > 2) it.copy(sets = it.sets - 1) else it }
            estimate = DurationEstimator.estimateExercises(exercises, warmupMinutes, cooldownMinutes)
        }
        while (estimate > maxMinutes && exercises.any { it.restSeconds > 30 && it.timeMinutes == null }) {
            exercises = exercises.map {
                if (it.timeMinutes == null && it.restSeconds > 30) {
                    it.copy(restSeconds = (it.restSeconds - 15).coerceAtLeast(30))
                } else it
            }
            estimate = DurationEstimator.estimateExercises(exercises, warmupMinutes, cooldownMinutes)
        }
        exercises.forEach { exercise -> weeklyUse[exercise.exerciseId] = (weeklyUse[exercise.exerciseId] ?: 0) + 1 }

        val date = input.weekStart.plusDays(day.dayOfWeek.value.toLong() - 1)
        return WorkoutSession(
            id = System.currentTimeMillis() + index * 10_000L,
            date = date,
            name = blueprint.name,
            objective = blueprint.objective,
            estimatedMinutes = estimate,
            location = location,
            warmup = warmupFor(blueprint),
            exercises = exercises,
            cooldown = listOf(
                "3 min de caminata suave o respiración tranquila.",
                "Estiramientos suaves de los músculos trabajados, sin dolor."
            ),
            status = SessionStatus.PLANNED,
            notes = if (exercises.size == 1) "Sesión exprés: el tiempo disponible es muy ajustado." else ""
        )
    }

    private fun recoverySession(input: PlanGenerationInput, day: DayAvailability, index: Int): WorkoutSession {
        val date = input.weekStart.plusDays(day.dayOfWeek.value.toLong() - 1)
        return WorkoutSession(
            id = System.currentTimeMillis() + index * 10_000L + 7,
            date = date,
            name = "Recuperación activa",
            objective = "Movilidad suave y descanso",
            estimatedMinutes = minOf(day.maxMinutes, 20),
            location = LocationType.HOME,
            warmup = listOf("Respiración diafragmática durante 2 minutos."),
            exercises = emptyList(),
            cooldown = listOf("Caminata suave de 10-20 minutos si te sienta bien."),
            status = SessionStatus.PLANNED
        )
    }

    private fun filterCandidates(
        input: PlanGenerationInput,
        profile: EquipmentProfile,
        location: LocationType
    ): List<Exercise> {
        val availableEquipment = profile.items + EquipmentType.BODYWEIGHT
        val restrictionTags = SafetyGate.restrictionTags(input.health)
        val avoid = (input.experience.exercisesToAvoid + input.health.avoidedExercises)
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        return input.exercises.filter { exercise ->
            exercise.locations.contains(location) &&
                exercise.equipment.all { it in availableEquipment } &&
                difficultyAllowed(exercise.difficulty, input.profile.experienceLevel) &&
                exercise.restrictionTags.intersect(restrictionTags).isEmpty() &&
                avoid.none { term ->
                    exercise.nameEs.lowercase().contains(term) ||
                        exercise.nameEn.lowercase().contains(term) ||
                        exercise.id.lowercase().contains(term)
                } &&
                goalAllowed(exercise, input.profile.primaryGoal)
        }
    }

    private fun goalAllowed(exercise: Exercise, goal: Goal): Boolean {
        if (goal == Goal.MOBILITY) {
            return exercise.type == ExerciseType.MOBILITY || exercise.pattern == MovementPattern.CORE || exercise.type == ExerciseType.CARDIO
        }
        if (goal == Goal.CARDIO) {
            return exercise.pattern == MovementPattern.CARDIO || exercise.type == ExerciseType.COMPOUND || exercise.type == ExerciseType.CARDIO
        }
        return true
    }

    private fun difficultyAllowed(exercise: Difficulty, level: ExperienceLevel): Boolean {
        return exercise.ordinal <= level.ordinal
    }

    private fun candidateComparator(input: PlanGenerationInput): Comparator<Exercise> {
        return compareByDescending<Exercise> { score(it, input) }.thenBy { it.id }
    }

    private fun score(exercise: Exercise, input: PlanGenerationInput): Int {
        var score = 0
        if (exercise.primaryMuscle in input.experience.musclePreferences) score += 100
        if (input.profile.primaryGoal in exercise.goals) score += 20
        if (exercise.type == ExerciseType.COMPOUND) score += 10
        score += 5 - abs(exercise.difficulty.ordinal - input.profile.experienceLevel.ordinal)
        return score
    }

    private fun targetExerciseCount(maxMinutes: Int, goal: Goal): Int {
        val base = when {
            maxMinutes <= 20 -> 2
            maxMinutes <= 35 -> 3
            maxMinutes <= 50 -> 4
            maxMinutes <= 65 -> 5
            maxMinutes <= 80 -> 6
            else -> 7
        }
        return if (goal == Goal.MOBILITY) minOf(base, 4) else base
    }

    private fun schemeFor(
        goal: Goal,
        level: ExperienceLevel,
        exercise: Exercise,
        maxMinutes: Int
    ): RepScheme {
        if (exercise.type == ExerciseType.CARDIO) {
            val minutes = when {
                maxMinutes >= 75 -> 20
                maxMinutes >= 50 -> 15
                maxMinutes >= 35 -> 10
                else -> 8
            }
            return RepScheme(sets = 1, repsMin = 0, repsMax = 0, restSeconds = 0, timeMinutes = minutes)
        }
        if (exercise.type == ExerciseType.MOBILITY) {
            return RepScheme(sets = 2, repsMin = 8, repsMax = 10, restSeconds = 30)
        }
        val baseSets = when (level) {
            ExperienceLevel.BEGINNER -> 2
            ExperienceLevel.INTERMEDIATE -> 3
            ExperienceLevel.ADVANCED -> 4
        }
        val sets = (baseSets + if (exercise.type == ExerciseType.COMPOUND) 1 else 0).coerceIn(2, 5)
        return when (goal) {
            Goal.STRENGTH -> RepScheme(sets, 4, 6, 150)
            Goal.MUSCLE_GAIN -> RepScheme(sets, 8, 12, 75)
            Goal.FAT_LOSS -> RepScheme(sets, 12, 15, 45)
            Goal.CARDIO -> RepScheme((sets - 1).coerceAtLeast(2), 12, 15, 45)
            Goal.MOBILITY -> RepScheme(2, 8, 10, 30)
            Goal.GENERAL_FITNESS, Goal.STAY_ACTIVE -> RepScheme(sets, 8, 12, 60)
        }
    }

    private fun intensityNote(goal: Goal, level: ExperienceLevel): String {
        return when {
            goal == Goal.STRENGTH -> "RIR 2-3: termina con reserva, sin fallo."
            level == ExperienceLevel.BEGINNER -> "RIR 3-4: deberías poder hacer 3-4 repeticiones más."
            level == ExperienceLevel.INTERMEDIATE -> "RIR 2-3: técnica sólida antes que carga."
            else -> "RIR 1-2: intensidad alta sin sacrificar técnica."
        }
    }

    private fun warmupFor(blueprint: SessionBlueprint): List<String> {
        val general = when (blueprint.type) {
            SessionType.MOBILITY -> "5 min de movilidad articular y respiración controlada."
            SessionType.CARDIO -> "5 min de cardio suave y progresivo."
            else -> "5 min de cardio suave + movilidad de hombros, cadera y tobillos."
        }
        return listOf(
            general,
            "1 serie de aproximación con carga ligera en el primer ejercicio.",
            "Empieza la primera serie de trabajo sin dolor y con control."
        )
    }
}
