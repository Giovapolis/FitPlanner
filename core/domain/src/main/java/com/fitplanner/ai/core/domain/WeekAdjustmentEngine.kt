package com.fitplanner.ai.core.domain

import com.fitplanner.ai.core.model.Adjustment
import com.fitplanner.ai.core.model.ExerciseStatus
import com.fitplanner.ai.core.model.SessionStatus
import com.fitplanner.ai.core.model.TrainingPlan
import com.fitplanner.ai.core.model.TrainingWeek
import com.fitplanner.ai.core.model.WeekReview
import com.fitplanner.ai.core.model.WeekStatus
import com.fitplanner.ai.core.model.WorkoutSession

object WeekAdjustmentEngine {
    fun createNextWeek(
        plan: TrainingPlan,
        currentWeek: TrainingWeek,
        review: WeekReview
    ): TrainingPlan {
        val now = System.currentTimeMillis()
        val oldWeek = currentWeek.copy(
            status = WeekStatus.COMPLETED,
            review = review
        )
        val nextStart = currentWeek.endDate.plusDays(1)
        val nextSessions = currentWeek.sessions.mapIndexed { sessionIndex, session ->
            val shift = session.date.plusDays(7)
            val adjustedExercises = session.exercises.mapIndexed { exerciseIndex, exercise ->
                val newSets = when {
                    exercise.timeMinutes != null -> exercise.sets
                    review.hadPain || review.adjustment == Adjustment.REDUCE -> (exercise.sets - 1).coerceAtLeast(2)
                    review.adjustment == Adjustment.INCREASE &&
                        !review.hadPain &&
                        review.metDuration &&
                        review.perceivedDifficulty <= 3 &&
                        exerciseIndex < 2 -> (exercise.sets + 1).coerceAtMost(5)
                    else -> exercise.sets
                }
                val newRest = if (exercise.timeMinutes != null) {
                    exercise.restSeconds
                } else if (review.hadPain || review.adjustment == Adjustment.REDUCE) {
                    exercise.restSeconds + 15
                } else {
                    exercise.restSeconds
                }
                exercise.copy(
                    id = now + sessionIndex * 10_000L + exerciseIndex,
                    sets = newSets,
                    restSeconds = newRest,
                    status = ExerciseStatus.PENDING,
                    logs = emptyList(),
                    suggestedWeightKg = null
                )
            }
            val note = if (review.hadPain) {
                "Semana ajustada por molestias: reduce la carga y consulta si el dolor persiste."
            } else {
                session.notes
            }
            session.copy(
                id = now + sessionIndex * 10_000L,
                date = shift,
                estimatedMinutes = DurationEstimator.estimateExercises(
                    adjustedExercises,
                    DurationEstimator.warmupMinutes(session.estimatedMinutes.coerceAtLeast(15)),
                    DurationEstimator.cooldownMinutes(session.estimatedMinutes.coerceAtLeast(15))
                ),
                exercises = adjustedExercises,
                status = SessionStatus.PLANNED,
                startedAt = null,
                completedAt = null,
                notes = note
            )
        }
        val nextWeek = TrainingWeek(
            id = now + 1,
            weekNumber = currentWeek.weekNumber + 1,
            startDate = nextStart,
            endDate = nextStart.plusDays(6),
            focus = currentWeek.focus,
            status = WeekStatus.ACTIVE,
            sessions = nextSessions
        )
        return plan.copy(
            updatedAt = now,
            weeks = plan.weeks.map { if (it.id == currentWeek.id) oldWeek else it } + nextWeek
        )
    }

    fun applyIncreaseRule(weeks: List<TrainingWeek>): List<TrainingWeek> = weeks
}
