package com.fitplanner.ai.core.domain

import com.fitplanner.ai.core.model.BodyArea
import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.GenerationResult
import com.fitplanner.ai.core.model.HealthScreening
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlanGeneratorTest {
    @Test
    fun `generates a plan with available days and time budget`() {
        val result = PlanGenerator.generate(TestFixtures.input(days = 3, minutes = 45))
        assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
        val plan = (result as GenerationResult.Success).plan
        assertThat(plan.weeks).hasSize(1)
        assertThat(plan.weeks.first().sessions).hasSize(3)
        plan.weeks.first().sessions.forEach { session ->
            assertThat(session.exercises).isNotEmpty()
            assertThat(session.estimatedMinutes).isAtMost(45)
        }
    }

    @Test
    fun `blocks generation with red flag symptoms`() {
        val result = PlanGenerator.generate(
            TestFixtures.input(health = HealthScreening(hasRedFlags = true))
        )
        assertThat(result).isInstanceOf(GenerationResult.Blocked::class.java)
    }

    @Test
    fun `blocks generation with medical recommendation`() {
        val result = PlanGenerator.generate(
            TestFixtures.input(health = HealthScreening(hasMedicalRecommendation = true))
        )
        assertThat(result).isInstanceOf(GenerationResult.Blocked::class.java)
    }

    @Test
    fun `does not select exercises conflicting with declared restrictions`() {
        val result = PlanGenerator.generate(
            TestFixtures.input(
                health = HealthScreening(hasInjury = true, injuryAreas = setOf(BodyArea.KNEE))
            )
        )
        assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
        val plan = (result as GenerationResult.Success).plan
        val ids = plan.weeks.flatMap { it.sessions }.flatMap { it.exercises }.map { it.exerciseId }
        assertThat(ids).doesNotContain("squat")
        assertThat(ids).doesNotContain("lunge")
    }

    @Test
    fun `does not select equipment that is not available`() {
        val result = PlanGenerator.generate(
            TestFixtures.input(equipment = TestFixtures.equipmentHome())
        )
        val plan = (result as GenerationResult.Success).plan
        val ids = plan.weeks.flatMap { it.sessions }.flatMap { it.exercises }.map { it.exerciseId }
        assertThat(ids).doesNotContain("barbell")
        assertThat(ids).doesNotContain("cardio")
    }

    @Test
    fun `is deterministic for the same input`() {
        val first = PlanGenerator.generate(TestFixtures.input()) as GenerationResult.Success
        val second = PlanGenerator.generate(TestFixtures.input()) as GenerationResult.Success
        val firstIds = first.plan.weeks.flatMap { it.sessions }.flatMap { it.exercises }.map { it.exerciseId }
        val secondIds = second.plan.weeks.flatMap { it.sessions }.flatMap { it.exercises }.map { it.exerciseId }
        assertThat(firstIds).isEqualTo(secondIds)
    }

    @Test
    fun `respects tight 15 minute sessions`() {
        val result = PlanGenerator.generate(TestFixtures.input(days = 2, minutes = 15))
        assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
        val plan = (result as GenerationResult.Success).plan
        plan.weeks.first().sessions.filter { it.exercises.isNotEmpty() }.forEach { session ->
            assertThat(session.estimatedMinutes).isAtMost(15)
        }
    }

    @Test
    fun `blocks when there are no available days`() {
        val result = PlanGenerator.generate(TestFixtures.input(days = 0))
        assertThat(result).isInstanceOf(GenerationResult.Blocked::class.java)
    }
}
