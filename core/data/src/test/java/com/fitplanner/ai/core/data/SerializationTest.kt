package com.fitplanner.ai.core.data

import com.fitplanner.ai.core.data.seed.ExerciseSeed
import com.fitplanner.ai.core.model.EquipmentProfile
import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.ExerciseStatus
import com.fitplanner.ai.core.model.Goal
import com.fitplanner.ai.core.model.LocationType
import com.fitplanner.ai.core.model.MuscleGroup
import com.fitplanner.ai.core.model.PlanStatus
import com.fitplanner.ai.core.model.SessionExercise
import com.fitplanner.ai.core.model.SessionStatus
import com.fitplanner.ai.core.model.SetLog
import com.fitplanner.ai.core.model.TrainingPlan
import com.fitplanner.ai.core.model.TrainingWeek
import com.fitplanner.ai.core.model.WeekStatus
import com.fitplanner.ai.core.model.WorkoutSession
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class SerializationTest {
    @Test
    fun `plan round trips with dates and nested exercises`() {
        val sessionExercise = SessionExercise(
            id = 1,
            exerciseId = "bodyweight-squat",
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
            id = 10,
            date = LocalDate.of(2026, 10, 26),
            name = "Cuerpo completo",
            objective = "Fuerza",
            estimatedMinutes = 45,
            location = LocationType.HOME,
            exercises = listOf(sessionExercise),
            status = SessionStatus.COMPLETED,
            startedAt = 1000,
            completedAt = 2000
        )
        val week = TrainingWeek(
            id = 20,
            weekNumber = 1,
            startDate = LocalDate.of(2026, 10, 26),
            endDate = LocalDate.of(2026, 11, 1),
            focus = "Cuerpo completo",
            status = WeekStatus.ACTIVE,
            sessions = listOf(session)
        )
        val plan = TrainingPlan(
            id = 30,
            name = "Plan",
            status = PlanStatus.ACTIVE,
            weeks = listOf(week)
        )
        val json = AppMoshi.writeJson(plan)
        val parsed = AppMoshi.readJson<TrainingPlan>(json)
        assertThat(parsed).isNotNull()
        assertThat(parsed!!.weeks.first().startDate).isEqualTo(LocalDate.of(2026, 10, 26))
        assertThat(parsed.weeks.first().sessions.first().exercises.first().logs).hasSize(1)
    }

    @Test
    fun `equipment profile round trips generic set`() {
        val profile = EquipmentProfile(
            id = 1,
            name = "Casa",
            location = LocationType.HOME,
            items = setOf(EquipmentType.BODYWEIGHT, EquipmentType.DUMBBELLS, EquipmentType.BENCH)
        )
        val json = AppMoshi.writeJson(profile)
        val parsed = AppMoshi.readJson<EquipmentProfile>(json)
        assertThat(parsed).isNotNull()
        assertThat(parsed!!.items).containsExactly(
            EquipmentType.BODYWEIGHT,
            EquipmentType.DUMBBELLS,
            EquipmentType.BENCH
        )
    }

    @Test
    fun `seed catalog contains the required exercise library`() {
        assertThat(ExerciseSeed.all.size).isAtLeast(20)
        val ids = ExerciseSeed.all.map { it.id }
        assertThat(ids).containsAtLeast(
            "bodyweight-squat",
            "goblet-squat",
            "dumbbell-rdl",
            "glute-bridge",
            "push-up",
            "dumbbell-bench-press",
            "machine-chest-press",
            "dumbbell-row",
            "seated-cable-row",
            "lat-pulldown",
            "dumbbell-shoulder-press",
            "lateral-raise",
            "biceps-curl",
            "triceps-pushdown",
            "plank",
            "bird-dog",
            "lunge",
            "calf-raise",
            "treadmill-walk",
            "stationary-bike"
        )
        ExerciseSeed.all.forEach { exercise ->
            assertThat(exercise.nameEs).isNotEmpty()
            assertThat(exercise.steps).isNotEmpty()
            assertThat(exercise.keyPoints).isNotEmpty()
            assertThat(exercise.equipment).isNotEmpty()
            assertThat(exercise.locations).isNotEmpty()
            assertThat(exercise.goals).isNotEmpty()
            assertThat(exercise.goals).contains(Goal.GENERAL_FITNESS)
            assertThat(exercise.animationKey).isNotEmpty()
        }
    }
}
