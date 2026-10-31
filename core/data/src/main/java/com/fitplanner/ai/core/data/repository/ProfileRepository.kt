package com.fitplanner.ai.core.data.repository

import androidx.room.withTransaction
import com.fitplanner.ai.core.data.AppMoshi
import com.fitplanner.ai.core.data.db.FitPlannerDatabase
import com.fitplanner.ai.core.data.toDomain
import com.fitplanner.ai.core.data.toEntity
import com.fitplanner.ai.core.model.DayAvailability
import com.fitplanner.ai.core.model.EquipmentProfile
import com.fitplanner.ai.core.model.HealthScreening
import com.fitplanner.ai.core.model.OnboardingDraft
import com.fitplanner.ai.core.model.TrainingExperience
import com.fitplanner.ai.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val database: FitPlannerDatabase
) {
    private val dao = database.profileDao()

    val profile: Flow<UserProfile?> = dao.observeProfile().map { it?.toDomain(AppMoshi) }
    val health: Flow<HealthScreening?> = dao.observeHealth().map { it?.toDomain(AppMoshi) }
    val experience: Flow<TrainingExperience?> = dao.observeExperience().map { it?.toDomain(AppMoshi) }
    val availability: Flow<List<DayAvailability>> =
        dao.observeAvailability().map { list -> list.map { it.toDomain() } }
    val equipmentProfiles: Flow<List<EquipmentProfile>> =
        dao.observeEquipmentProfiles().map { list -> list.map { it.toDomain(AppMoshi) } }

    suspend fun saveProfile(profile: UserProfile) {
        dao.upsertProfile(profile.copy(updatedAt = System.currentTimeMillis()).toEntity(AppMoshi))
    }

    suspend fun saveHealth(health: HealthScreening) {
        dao.upsertHealth(health.toEntity(AppMoshi))
    }

    suspend fun saveExperience(experience: TrainingExperience) {
        dao.upsertExperience(experience.toEntity(AppMoshi))
    }

    suspend fun saveAvailability(availability: List<DayAvailability>) {
        dao.upsertAvailability(availability.map { it.toEntity() })
    }

    suspend fun saveEquipmentProfiles(profiles: List<EquipmentProfile>) {
        dao.upsertEquipmentProfiles(profiles.map { it.toEntity(AppMoshi) })
    }

    suspend fun saveOnboarding(draft: OnboardingDraft) {
        database.withTransaction {
            dao.upsertProfile(draft.profile.copy(updatedAt = System.currentTimeMillis()).toEntity(AppMoshi))
            dao.upsertHealth(draft.health.toEntity(AppMoshi))
            dao.upsertExperience(draft.experience.toEntity(AppMoshi))
            dao.upsertAvailability(draft.availability.map { it.toEntity() })
            dao.upsertEquipmentProfiles(draft.equipmentProfiles.map { it.toEntity(AppMoshi) })
        }
    }

    suspend fun loadOnboardingSnapshot(): OnboardingDraft {
        val profile = dao.getProfile()?.toDomain(AppMoshi) ?: UserProfile()
        val health = dao.getHealth()?.toDomain(AppMoshi) ?: HealthScreening()
        val experience = dao.getExperience()?.toDomain(AppMoshi) ?: TrainingExperience()
        val availability = dao.getAvailability().map { it.toDomain() }
        val equipment = dao.getEquipmentProfiles().map { it.toDomain(AppMoshi) }
        return OnboardingDraft(
            profile = profile,
            health = health,
            experience = experience,
            availability = availability,
            equipmentProfiles = equipment.ifEmpty { OnboardingDraft().equipmentProfiles }
        )
    }
}
