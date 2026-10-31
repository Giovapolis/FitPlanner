package com.fitplanner.ai.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitplanner.ai.core.data.db.FitPlannerDatabase
import com.fitplanner.ai.core.data.repository.ExerciseRepository
import com.fitplanner.ai.core.data.repository.ProfileRepository
import com.fitplanner.ai.core.model.DayAvailability
import com.fitplanner.ai.core.model.EquipmentProfile
import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.Goal
import com.fitplanner.ai.core.model.HealthScreening
import com.fitplanner.ai.core.model.LocationType
import com.fitplanner.ai.core.model.OnboardingDraft
import com.fitplanner.ai.core.model.SessionType
import com.fitplanner.ai.core.model.TrainingExperience
import com.fitplanner.ai.core.model.UserProfile
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RepositoryTest {
    private lateinit var database: FitPlannerDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitPlannerDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `onboarding snapshot survives local persistence`() = runTest {
        val repository = ProfileRepository(database)
        val draft = OnboardingDraft(
            profile = UserProfile(primaryGoal = Goal.STRENGTH),
            health = HealthScreening(hasInjury = true, injuryAreas = setOf(com.fitplanner.ai.core.model.BodyArea.KNEE)),
            experience = TrainingExperience(monthsTraining = 6),
            availability = DayOfWeek.values().map { day ->
                DayAvailability(
                    dayOfWeek = day,
                    available = day == DayOfWeek.MONDAY || day == DayOfWeek.WEDNESDAY,
                    maxMinutes = 45,
                    sessionType = SessionType.FULL_BODY
                )
            },
            equipmentProfiles = listOf(
                EquipmentProfile(
                    id = 1,
                    name = "Casa",
                    location = LocationType.HOME,
                    items = setOf(EquipmentType.BODYWEIGHT, EquipmentType.DUMBBELLS)
                )
            )
        )
        repository.saveOnboarding(draft)
        val loaded = repository.loadOnboardingSnapshot()
        assertThat(loaded.profile?.primaryGoal).isEqualTo(Goal.STRENGTH)
        assertThat(loaded.health?.hasInjury).isTrue()
        assertThat(loaded.availability.count { it.available }).isEqualTo(2)
        assertThat(loaded.equipmentProfiles.first().items).contains(EquipmentType.DUMBBELLS)
    }

    @Test
    fun `catalog seeding is idempotent`() = runTest {
        val repository = ExerciseRepository(database.exerciseDao())
        repository.seedCatalog()
        val first = repository.getAll()
        repository.seedCatalog()
        val second = repository.getAll()
        assertThat(first.size).isAtLeast(20)
        assertThat(second.size).isEqualTo(first.size)
        assertThat(second.map { it.id }).contains("bodyweight-squat")
    }
}
