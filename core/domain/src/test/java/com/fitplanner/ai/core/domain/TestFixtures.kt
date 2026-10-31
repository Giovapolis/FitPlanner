package com.fitplanner.ai.core.domain

import com.fitplanner.ai.core.model.AgeRange
import com.fitplanner.ai.core.model.BodyArea
import com.fitplanner.ai.core.model.DayAvailability
import com.fitplanner.ai.core.model.Difficulty
import com.fitplanner.ai.core.model.EquipmentProfile
import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.Exercise
import com.fitplanner.ai.core.model.ExerciseStatus
import com.fitplanner.ai.core.model.ExerciseType
import com.fitplanner.ai.core.model.ExperienceLevel
import com.fitplanner.ai.core.model.Goal
import com.fitplanner.ai.core.model.HealthScreening
import com.fitplanner.ai.core.model.LocationType
import com.fitplanner.ai.core.model.MovementPattern
import com.fitplanner.ai.core.model.MuscleGroup
import com.fitplanner.ai.core.model.PlanGenerationInput
import com.fitplanner.ai.core.model.PlanStatus
import com.fitplanner.ai.core.model.SessionExercise
import com.fitplanner.ai.core.model.SetLog
import com.fitplanner.ai.core.model.SessionStatus
import com.fitplanner.ai.core.model.SessionType
import com.fitplanner.ai.core.model.TrainingExperience
import com.fitplanner.ai.core.model.TrainingPlan
import com.fitplanner.ai.core.model.TrainingWeek
import com.fitplanner.ai.core.model.UserProfile
import com.fitplanner.ai.core.model.WeekStatus
import com.fitplanner.ai.core.model.WorkoutSession
import java.time.DayOfWeek
import java.time.LocalDate

object TestFixtures {
    val monday: LocalDate = LocalDate.of(2026, 10, 26)

    fun exercise(
        id: String,
        name: String,
        muscle: MuscleGroup,
        pattern: MovementPattern,
        equipment: Set<EquipmentType> = setOf(EquipmentType.BODYWEIGHT),
        difficulty: Difficulty = Difficulty.BEGINNER,
        restrictions: Set<String> = emptySet(),
        type: ExerciseType = ExerciseType.COMPOUND
    ) = Exercise(
        id = id,
        nameEs = name,
        primaryMuscle = muscle,
        equipment = equipment,
        locations = setOf(LocationType.HOME, LocationType.GYM),
        difficulty = difficulty,
        pattern = pattern,
        goals = setOf(Goal.GENERAL_FITNESS, Goal.STRENGTH, Goal.MUSCLE_GAIN, Goal.FAT_LOSS),
        type = type,
        summary = "Resumen de $name",
        keyPoints = listOf("Controla el movimiento"),
        restrictionTags = restrictions
    )

    val exercises = listOf(
        exercise("squat", "Sentadilla", MuscleGroup.QUADS, MovementPattern.SQUAT, restrictions = setOf("KNEE")),
        exercise("hinge", "Bisagra", MuscleGroup.HAMSTRINGS, MovementPattern.HINGE),
        exercise("push", "Flexión", MuscleGroup.CHEST, MovementPattern.HORIZONTAL_PUSH),
        exercise("pull", "Remo", MuscleGroup.BACK, MovementPattern.HORIZONTAL_PULL, setOf(EquipmentType.BODYWEIGHT, EquipmentType.DUMBBELLS)),
        exercise("ohp", "Press hombros", MuscleGroup.SHOULDERS, MovementPattern.VERTICAL_PUSH, setOf(EquipmentType.DUMBBELLS), Difficulty.INTERMEDIATE),
        exercise("pullup", "Jalón", MuscleGroup.LATS, MovementPattern.VERTICAL_PULL, setOf(EquipmentType.PULL_UP_BAR), Difficulty.INTERMEDIATE),
        exercise("lunge", "Zancada", MuscleGroup.QUADS, MovementPattern.LUNGE, restrictions = setOf("KNEE")),
        exercise("core", "Plancha", MuscleGroup.CORE, MovementPattern.CORE, type = ExerciseType.BODYWEIGHT),
        exercise("bridge", "Puente", MuscleGroup.GLUTES, MovementPattern.HINGE),
        exercise("curl", "Curl", MuscleGroup.BICEPS, MovementPattern.HORIZONTAL_PULL, setOf(EquipmentType.DUMBBELLS), type = ExerciseType.ISOLATION),
        exercise("barbell", "Sentadilla barra", MuscleGroup.QUADS, MovementPattern.SQUAT, setOf(EquipmentType.BARBELL), Difficulty.INTERMEDIATE),
        exercise("cardio", "Cinta", MuscleGroup.CARDIO, MovementPattern.CARDIO, setOf(EquipmentType.TREADMILL), type = ExerciseType.CARDIO)
    )

    fun availability(days: Int = 3, minutes: Int = 45): List<DayAvailability> =
        DayOfWeek.values().mapIndexed { index, day ->
            DayAvailability(
                dayOfWeek = day,
                available = index < days,
                maxMinutes = minutes,
                sessionType = SessionType.FULL_BODY
            )
        }

    fun equipmentHome() = listOf(
        EquipmentProfile(
            id = 1,
            name = "Casa",
            location = LocationType.HOME,
            items = setOf(EquipmentType.BODYWEIGHT, EquipmentType.DUMBBELLS)
        )
    )

    fun equipmentGym() = listOf(
        EquipmentProfile(
            id = 2,
            name = "Gimnasio",
            location = LocationType.GYM,
            items = setOf(
                EquipmentType.BODYWEIGHT,
                EquipmentType.DUMBBELLS,
                EquipmentType.BARBELL,
                EquipmentType.RACK,
                EquipmentType.TREADMILL
            )
        )
    )

    fun input(
        days: Int = 3,
        minutes: Int = 45,
        level: ExperienceLevel = ExperienceLevel.BEGINNER,
        goal: Goal = Goal.GENERAL_FITNESS,
        health: HealthScreening = HealthScreening(),
        equipment: List<EquipmentProfile> = equipmentHome(),
        exercises: List<Exercise> = this.exercises
    ) = PlanGenerationInput(
        profile = UserProfile(
            ageRange = AgeRange.AGE_25_34,
            primaryGoal = goal,
            experienceLevel = level,
            unitSystem = com.fitplanner.ai.core.model.UnitSystem.METRIC
        ),
        health = health,
        experience = TrainingExperience(intensityPreference = com.fitplanner.ai.core.model.IntensityPreference.MODERATE),
        availability = availability(days, minutes),
        equipmentProfiles = equipment,
        exercises = exercises,
        weekStart = monday,
        weekNumber = 1
    )

    fun planWithSession(): TrainingPlan {
        val sessionExercise = SessionExercise(
            id = 10L,
            exerciseId = "squat",
            exerciseName = "Sentadilla",
            primaryMuscle = MuscleGroup.QUADS,
            orderIndex = 0,
            sets = 3,
            repsMin = 8,
            repsMax = 12,
            restSeconds = 60,
            status = ExerciseStatus.COMPLETED,
            logs = listOf(SetLog(id = 1, setNumber = 1, reps = 10, weightKg = 20.0))
        )
        val session = WorkoutSession(
            id = 100L,
            date = monday,
            name = "Cuerpo completo",
            objective = "Fuerza",
            estimatedMinutes = 45,
            location = LocationType.HOME,
            exercises = listOf(sessionExercise),
            status = SessionStatus.COMPLETED,
            startedAt = 1_000L,
            completedAt = 2_000L
        )
        val week = TrainingWeek(
            id = 200L,
            weekNumber = 1,
            startDate = monday,
            endDate = monday.plusDays(6),
            focus = "Cuerpo completo",
            status = WeekStatus.ACTIVE,
            sessions = listOf(session)
        )
        return TrainingPlan(
            id = 300L,
            name = "Plan de prueba",
            status = PlanStatus.ACTIVE,
            weeks = listOf(week)
        )
    }
}
