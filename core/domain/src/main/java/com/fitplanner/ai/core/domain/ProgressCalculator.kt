package com.fitplanner.ai.core.domain

import com.fitplanner.ai.core.model.DashboardData
import com.fitplanner.ai.core.model.MuscleGroup
import com.fitplanner.ai.core.model.PersonalRecord
import com.fitplanner.ai.core.model.PlanStatus
import com.fitplanner.ai.core.model.SessionStatus
import com.fitplanner.ai.core.model.TrainingPlan
import com.fitplanner.ai.core.model.TrainingWeek
import com.fitplanner.ai.core.model.UserProfile
import com.fitplanner.ai.core.model.WeeklyProgress
import com.fitplanner.ai.core.model.WorkoutSession
import java.time.LocalDate

object ProgressCalculator {
    fun buildDashboard(
        plan: TrainingPlan?,
        profile: UserProfile?,
        today: LocalDate = LocalDate.now()
    ): DashboardData {
        val activePlan = plan?.takeIf { it.status == PlanStatus.ACTIVE }
        val week = activePlan?.weeks?.firstOrNull { today in it.startDate..it.endDate }
            ?: activePlan?.activeWeek()
        val sessions = week?.sessions.orEmpty()
        val todaySession = sessions.firstOrNull { it.date == today }
        val nextSession = sessions
            .filter { it.date > today && it.status != SessionStatus.COMPLETED && it.exercises.isNotEmpty() }
            .minByOrNull { it.date }
        val lastCompleted = activePlan?.weeks.orEmpty()
            .flatMap { it.sessions }
            .filter { it.status == SessionStatus.COMPLETED }
            .maxByOrNull { it.completedAt ?: 0L }
        return DashboardData(
            profile = profile,
            activePlan = activePlan,
            activeWeek = week,
            todaySession = todaySession,
            nextSession = nextSession,
            lastCompletedSession = lastCompleted,
            weeklyCompliance = week?.compliancePercent ?: 0,
            completedThisWeek = week?.completedSessions ?: 0,
            plannedThisWeek = week?.plannedSessions ?: 0,
            muscleVolume = muscleVolume(week)
        )
    }

    fun weeklyProgress(plan: TrainingPlan?, today: LocalDate = LocalDate.now()): WeeklyProgress {
        val week = plan?.weeks?.firstOrNull { today in it.startDate..it.endDate } ?: plan?.activeWeek()
        return WeeklyProgress(
            week = week,
            compliancePercent = week?.compliancePercent ?: 0,
            completedSessions = week?.completedSessions ?: 0,
            plannedSessions = week?.plannedSessions ?: 0,
            muscleVolume = muscleVolume(week),
            personalRecords = personalRecords(plan)
        )
    }

    fun muscleVolume(week: TrainingWeek?): Map<MuscleGroup, Int> {
        if (week == null) return emptyMap()
        val result = mutableMapOf<MuscleGroup, Int>()
        week.sessions
            .filter { it.status == SessionStatus.COMPLETED }
            .flatMap { it.exercises }
            .forEach { exercise ->
                val sets = exercise.logs.size
                if (sets > 0) result[exercise.primaryMuscle] = (result[exercise.primaryMuscle] ?: 0) + sets
            }
        return result
    }

    fun personalRecords(plan: TrainingPlan?): List<PersonalRecord> {
        if (plan == null) return emptyList()
        val best = mutableMapOf<String, PersonalRecord>()
        plan.weeks.forEach { week ->
            week.sessions.forEach { session ->
                session.exercises.forEach { exercise ->
                    exercise.logs.forEach { log ->
                        val weight = log.weightKg ?: return@forEach
                        if (weight <= 0) return@forEach
                        val current = best[exercise.exerciseId]
                        if (current == null || weight > current.weightKg) {
                            best[exercise.exerciseId] = PersonalRecord(
                                exerciseId = exercise.exerciseId,
                                exerciseName = exercise.exerciseName,
                                weightKg = weight,
                                reps = log.reps,
                                date = session.date
                            )
                        }
                    }
                }
            }
        }
        return best.values.sortedByDescending { it.date }
    }

    fun sessionById(plan: TrainingPlan?, sessionId: Long): WorkoutSession? {
        return plan?.weeks?.flatMap { it.sessions }?.firstOrNull { it.id == sessionId }
    }

    fun updateSession(plan: TrainingPlan, session: WorkoutSession): TrainingPlan {
        return plan.copy(
            updatedAt = System.currentTimeMillis(),
            weeks = plan.weeks.map { week ->
                if (week.sessions.any { it.id == session.id }) {
                    week.copy(sessions = week.sessions.map { if (it.id == session.id) session else it })
                } else {
                    week
                }
            }
        )
    }
}
