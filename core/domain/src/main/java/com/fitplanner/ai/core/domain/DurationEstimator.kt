package com.fitplanner.ai.core.domain

import com.fitplanner.ai.core.model.SessionExercise
import com.fitplanner.ai.core.model.WorkoutSession
import kotlin.math.ceil

object DurationEstimator {
    private const val TRANSITION_SECONDS = 45
    private const val SECONDS_PER_REP = 3

    fun estimateExercises(exercises: List<SessionExercise>, warmupMinutes: Int, cooldownMinutes: Int): Int {
        var seconds = (warmupMinutes + cooldownMinutes) * 60
        exercises.forEach { exercise ->
            val timedMinutes = exercise.timeMinutes
            val workSeconds = if (timedMinutes != null) {
                timedMinutes * 60
            } else {
                exercise.sets * (((exercise.repsMin + exercise.repsMax) / 2) * SECONDS_PER_REP + exercise.restSeconds)
            }
            seconds += workSeconds + TRANSITION_SECONDS
        }
        return ceil(seconds / 60.0).toInt()
    }

    fun estimate(session: WorkoutSession): Int {
        val warmup = if (session.estimatedMinutes <= 30) 4 else 7
        val cooldown = if (session.estimatedMinutes <= 30) 2 else 4
        return estimateExercises(session.exercises, warmup, cooldown)
    }

    fun warmupMinutes(maxMinutes: Int): Int = if (maxMinutes <= 30) 4 else 7
    fun cooldownMinutes(maxMinutes: Int): Int = if (maxMinutes <= 30) 2 else 4
}
