package com.fitplanner.ai.core.data.repository

import com.fitplanner.ai.core.data.AppMoshi
import com.fitplanner.ai.core.data.db.PlanDao
import com.fitplanner.ai.core.data.toDomain
import com.fitplanner.ai.core.data.toEntity
import com.fitplanner.ai.core.domain.DurationEstimator
import com.fitplanner.ai.core.domain.PlanGenerator
import com.fitplanner.ai.core.domain.WeekAdjustmentEngine
import com.fitplanner.ai.core.model.Exercise
import com.fitplanner.ai.core.model.ExerciseStatus
import com.fitplanner.ai.core.model.GenerationResult
import com.fitplanner.ai.core.model.PlanGenerationInput
import com.fitplanner.ai.core.model.SessionExercise
import com.fitplanner.ai.core.model.SessionStatus
import com.fitplanner.ai.core.model.SetLog
import com.fitplanner.ai.core.model.TrainingPlan
import com.fitplanner.ai.core.model.WeekReview
import com.fitplanner.ai.core.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepository @Inject constructor(
    private val dao: PlanDao
) {
    val activePlan: Flow<TrainingPlan?> = dao.observeActive()
        .map { it?.toDomain(AppMoshi) }

    val allPlans: Flow<List<TrainingPlan>> = dao.observeAll()
        .map { list -> list.mapNotNull { it.toDomain(AppMoshi) } }

    suspend fun getActivePlan(): TrainingPlan? = dao.getActive()?.toDomain(AppMoshi)

    suspend fun generateAndSave(input: PlanGenerationInput): GenerationResult {
        val result = PlanGenerator.generate(input)
        if (result is GenerationResult.Success) {
            dao.archiveActive()
            dao.upsert(result.plan.toEntity(AppMoshi))
        }
        return result
    }

    suspend fun updatePlan(plan: TrainingPlan) {
        dao.upsert(plan.copy(updatedAt = System.currentTimeMillis()).toEntity(AppMoshi))
    }

    suspend fun startSession(plan: TrainingPlan, sessionId: Long): TrainingPlan {
        val now = System.currentTimeMillis()
        val updated = plan.updateSession(sessionId) { session ->
            session.copy(status = SessionStatus.IN_PROGRESS, startedAt = session.startedAt ?: now)
        }
        updatePlan(updated)
        return updated
    }

    suspend fun logSet(
        plan: TrainingPlan,
        sessionId: Long,
        sessionExerciseId: Long,
        reps: Int,
        weightKg: Double?,
        rpe: Int?
    ): TrainingPlan {
        val now = System.currentTimeMillis()
        val updated = plan.updateSession(sessionId) { session ->
            val exercises = session.exercises.map { exercise ->
                if (exercise.id != sessionExerciseId) return@map exercise
                val log = SetLog(
                    id = now,
                    setNumber = exercise.logs.size + 1,
                    reps = reps,
                    weightKg = weightKg,
                    rpe = rpe
                )
                exercise.copy(
                    logs = exercise.logs + log,
                    status = if (exercise.logs.size + 1 >= exercise.sets) ExerciseStatus.COMPLETED else ExerciseStatus.PENDING
                )
            }
            session.copy(
                exercises = exercises,
                status = SessionStatus.IN_PROGRESS,
                startedAt = session.startedAt ?: now
            )
        }
        updatePlan(updated)
        return updated
    }

    suspend fun finishSession(plan: TrainingPlan, sessionId: Long): TrainingPlan {
        val updated = plan.updateSession(sessionId) { session ->
            session.copy(
                status = SessionStatus.COMPLETED,
                completedAt = System.currentTimeMillis()
            )
        }
        updatePlan(updated)
        return updated
    }

    suspend fun skipExercise(plan: TrainingPlan, sessionId: Long, sessionExerciseId: Long): TrainingPlan {
        val updated = plan.updateSession(sessionId) { session ->
            session.copy(
                exercises = session.exercises.map {
                    if (it.id == sessionExerciseId) it.copy(status = ExerciseStatus.SKIPPED) else it
                }
            )
        }
        updatePlan(updated)
        return updated
    }

    suspend fun substituteExercise(
        plan: TrainingPlan,
        sessionId: Long,
        oldSessionExerciseId: Long,
        newExercise: Exercise
    ): TrainingPlan {
        val updated = plan.updateSession(sessionId) { session ->
            val newExercises = session.exercises.map { current ->
                if (current.id != oldSessionExerciseId) return@map current
                current.copy(
                    exerciseId = newExercise.id,
                    exerciseName = newExercise.nameEs,
                    primaryMuscle = newExercise.primaryMuscle,
                    techniqueCue = newExercise.keyPoints.firstOrNull() ?: newExercise.summary,
                    status = ExerciseStatus.PENDING,
                    logs = emptyList(),
                    substitutedFrom = current.exerciseName
                )
            }
            session.copy(
                exercises = newExercises,
                estimatedMinutes = DurationEstimator.estimateExercises(
                    newExercises,
                    DurationEstimator.warmupMinutes(session.estimatedMinutes.coerceAtLeast(15)),
                    DurationEstimator.cooldownMinutes(session.estimatedMinutes.coerceAtLeast(15))
                )
            )
        }
        updatePlan(updated)
        return updated
    }

    suspend fun saveWeekReview(
        plan: TrainingPlan,
        weekId: Long,
        review: WeekReview
    ): TrainingPlan {
        val week = plan.weeks.firstOrNull { it.id == weekId } ?: return plan
        val updated = WeekAdjustmentEngine.createNextWeek(plan, week, review)
        updatePlan(updated)
        return updated
    }

    suspend fun clearHistory() {
        dao.clear()
    }

    private fun TrainingPlan.updateSession(
        sessionId: Long,
        transform: (WorkoutSession) -> WorkoutSession
    ): TrainingPlan {
        return copy(
            updatedAt = System.currentTimeMillis(),
            weeks = weeks.map { week ->
                if (week.sessions.any { it.id == sessionId }) {
                    week.copy(sessions = week.sessions.map { if (it.id == sessionId) transform(it) else it })
                } else {
                    week
                }
            }
        )
    }

    fun findSession(plan: TrainingPlan?, sessionId: Long): WorkoutSession? {
        return plan?.weeks?.flatMap { it.sessions }?.firstOrNull { it.id == sessionId }
    }

    fun findExercise(session: WorkoutSession?, sessionExerciseId: Long): SessionExercise? {
        return session?.exercises?.firstOrNull { it.id == sessionExerciseId }
    }
}
