package com.fitplanner.ai.core.domain

import com.fitplanner.ai.core.model.Adjustment
import com.fitplanner.ai.core.model.WeekReview
import com.fitplanner.ai.core.model.WeekStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WeekAdjustmentEngineTest {
    @Test
    fun `pain reduces volume and keeps previous logs`() {
        val plan = TestFixtures.planWithSession()
        val week = plan.weeks.first()
        val updated = WeekAdjustmentEngine.createNextWeek(
            plan = plan,
            currentWeek = week,
            review = WeekReview(
                completedSessions = 1,
                perceivedDifficulty = 4,
                hadPain = true,
                metDuration = false,
                adjustment = Adjustment.INCREASE
            )
        )
        assertThat(updated.weeks).hasSize(2)
        val old = updated.weeks.first()
        assertThat(old.status).isEqualTo(WeekStatus.COMPLETED)
        assertThat(old.review).isNotNull()
        assertThat(old.sessions.first().exercises.first().logs).hasSize(1)
        val next = updated.weeks.last()
        assertThat(next.status).isEqualTo(WeekStatus.ACTIVE)
        assertThat(next.sessions.first().exercises.first().sets).isEqualTo(2)
        assertThat(next.sessions.first().exercises.first().logs).isEmpty()
        assertThat(next.sessions.first().exercises.first().suggestedWeightKg).isNull()
    }

    @Test
    fun `increase adds at most one set and never a weight`() {
        val plan = TestFixtures.planWithSession()
        val week = plan.weeks.first()
        val updated = WeekAdjustmentEngine.createNextWeek(
            plan = plan,
            currentWeek = week,
            review = WeekReview(
                completedSessions = 1,
                perceivedDifficulty = 2,
                hadPain = false,
                metDuration = true,
                adjustment = Adjustment.INCREASE
            )
        )
        val nextExercise = updated.weeks.last().sessions.first().exercises.first()
        assertThat(nextExercise.sets).isEqualTo(4)
        assertThat(nextExercise.suggestedWeightKg).isNull()
    }
}
