package com.fitplanner.ai.core.domain

import com.fitplanner.ai.core.model.MuscleGroup
import com.fitplanner.ai.core.model.SessionExercise
import com.fitplanner.ai.core.model.SessionStatus
import com.fitplanner.ai.core.model.WorkoutSession
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class DurationEstimatorTest {
    @Test
    fun `counts warmup cooldown work and rest`() {
        val exercise = SessionExercise(
            id = 1,
            exerciseId = "squat",
            exerciseName = "Sentadilla",
            primaryMuscle = MuscleGroup.QUADS,
            orderIndex = 0,
            sets = 3,
            repsMin = 8,
            repsMax = 12,
            restSeconds = 60
        )
        val estimate = DurationEstimator.estimateExercises(listOf(exercise), warmupMinutes = 5, cooldownMinutes = 3)
        assertThat(estimate).isAtLeast(8)
        assertThat(estimate).isAtMost(20)
    }

    @Test
    fun `timed cardio uses minutes`() {
        val cardio = SessionExercise(
            id = 2,
            exerciseId = "bike",
            exerciseName = "Bici",
            primaryMuscle = MuscleGroup.CARDIO,
            orderIndex = 0,
            sets = 1,
            repsMin = 0,
            repsMax = 0,
            restSeconds = 0,
            timeMinutes = 15
        )
        val estimate = DurationEstimator.estimateExercises(listOf(cardio), warmupMinutes = 5, cooldownMinutes = 3)
        assertThat(estimate).isAtLeast(20)
    }

    @Test
    fun `estimates a session`() {
        val session = WorkoutSession(
            id = 1,
            date = LocalDate.now(),
            name = "Test",
            objective = "Test",
            estimatedMinutes = 45,
            location = com.fitplanner.ai.core.model.LocationType.HOME,
            exercises = emptyList(),
            status = SessionStatus.PLANNED
        )
        assertThat(DurationEstimator.estimate(session)).isAtLeast(6)
    }
}
