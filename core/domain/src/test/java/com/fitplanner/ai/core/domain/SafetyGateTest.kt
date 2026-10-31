package com.fitplanner.ai.core.domain

import com.fitplanner.ai.core.model.BodyArea
import com.fitplanner.ai.core.model.HealthScreening
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SafetyGateTest {
    @Test
    fun `red flags block generation`() {
        val result = SafetyGate.evaluate(HealthScreening(hasRedFlags = true))
        assertThat(result).isInstanceOf(SafetyGateResult.Blocked::class.java)
    }

    @Test
    fun `medical recommendation blocks generation`() {
        val result = SafetyGate.evaluate(HealthScreening(hasMedicalRecommendation = true))
        assertThat(result).isInstanceOf(SafetyGateResult.Blocked::class.java)
    }

    @Test
    fun `declared injury allows generation with caution`() {
        val result = SafetyGate.evaluate(
            HealthScreening(hasInjury = true, injuryAreas = setOf(BodyArea.KNEE))
        )
        assertThat(result).isInstanceOf(SafetyGateResult.Allowed::class.java)
        assertThat((result as SafetyGateResult.Allowed).warnings).isNotEmpty()
    }

    @Test
    fun `restriction tags map declared areas`() {
        val tags = SafetyGate.restrictionTags(
            HealthScreening(hasInjury = true, injuryAreas = setOf(BodyArea.KNEE, BodyArea.SHOULDER))
        )
        assertThat(tags).containsExactly("KNEE", "SHOULDER")
    }
}
