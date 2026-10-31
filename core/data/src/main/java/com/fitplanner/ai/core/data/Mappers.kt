package com.fitplanner.ai.core.data

import com.fitplanner.ai.core.data.db.AvailabilityEntity
import com.fitplanner.ai.core.data.db.BodyMetricEntity
import com.fitplanner.ai.core.data.db.EquipmentProfileEntity
import com.fitplanner.ai.core.data.db.ExerciseEntity
import com.fitplanner.ai.core.data.db.ExperienceEntity
import com.fitplanner.ai.core.data.db.HealthEntity
import com.fitplanner.ai.core.data.db.PlanEntity
import com.fitplanner.ai.core.data.db.ProfileEntity
import com.fitplanner.ai.core.model.BodyMetric
import com.fitplanner.ai.core.model.DayAvailability
import com.fitplanner.ai.core.model.Difficulty
import com.fitplanner.ai.core.model.EquipmentProfile
import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.Exercise
import com.fitplanner.ai.core.model.ExerciseType
import com.fitplanner.ai.core.model.ExperienceLevel
import com.fitplanner.ai.core.model.Goal
import com.fitplanner.ai.core.model.HealthScreening
import com.fitplanner.ai.core.model.LocationType
import com.fitplanner.ai.core.model.MovementPattern
import com.fitplanner.ai.core.model.MuscleGroup
import com.fitplanner.ai.core.model.PlanStatus
import com.fitplanner.ai.core.model.SessionType
import com.fitplanner.ai.core.model.TrainingExperience
import com.fitplanner.ai.core.model.TrainingPlan
import com.fitplanner.ai.core.model.UserProfile
import com.squareup.moshi.Types
import java.time.DayOfWeek
import java.time.LocalDate

fun UserProfile.toEntity(moshi: com.squareup.moshi.Moshi) = ProfileEntity(
    json = moshi.writeJson(this),
    updatedAt = updatedAt
)

fun ProfileEntity.toDomain(moshi: com.squareup.moshi.Moshi): UserProfile? =
    moshi.readJson(json)

fun HealthScreening.toEntity(moshi: com.squareup.moshi.Moshi) = HealthEntity(
    json = moshi.writeJson(this),
    updatedAt = System.currentTimeMillis()
)

fun HealthEntity.toDomain(moshi: com.squareup.moshi.Moshi): HealthScreening? =
    moshi.readJson(json)

fun TrainingExperience.toEntity(moshi: com.squareup.moshi.Moshi) = ExperienceEntity(
    json = moshi.writeJson(this),
    updatedAt = System.currentTimeMillis()
)

fun ExperienceEntity.toDomain(moshi: com.squareup.moshi.Moshi): TrainingExperience? =
    moshi.readJson(json)

fun DayAvailability.toEntity() = AvailabilityEntity(
    dayOfWeek = dayOfWeek.value,
    available = available,
    preferredTime = preferredTime,
    maxMinutes = maxMinutes,
    sessionType = sessionType.name,
    allowLightActivity = allowLightActivity
)

fun AvailabilityEntity.toDomain(): DayAvailability = DayAvailability(
    dayOfWeek = DayOfWeek.of(dayOfWeek),
    available = available,
    preferredTime = preferredTime,
    maxMinutes = maxMinutes,
    sessionType = runCatching { SessionType.valueOf(sessionType) }.getOrDefault(SessionType.FULL_BODY),
    allowLightActivity = allowLightActivity
)

fun EquipmentProfile.toEntity(moshi: com.squareup.moshi.Moshi): EquipmentProfileEntity {
    val adapter = moshi.adapter<Set<EquipmentType>>(
        Types.newParameterizedType(Set::class.java, EquipmentType::class.java)
    )
    return EquipmentProfileEntity(
        id = id,
        name = name,
        location = location.name,
        itemsJson = adapter.toJson(items),
        isDefault = isDefault
    )
}

fun EquipmentProfileEntity.toDomain(moshi: com.squareup.moshi.Moshi): EquipmentProfile {
    val adapter = moshi.adapter<Set<EquipmentType>>(
        Types.newParameterizedType(Set::class.java, EquipmentType::class.java)
    )
    return EquipmentProfile(
        id = id,
        name = name,
        location = runCatching { LocationType.valueOf(location) }.getOrDefault(LocationType.HOME),
        items = runCatching { adapter.fromJson(itemsJson) }.getOrNull() ?: emptySet(),
        isDefault = isDefault
    )
}

fun Exercise.toEntity(moshi: com.squareup.moshi.Moshi) = ExerciseEntity(
    id = id,
    nameEs = nameEs,
    nameEn = nameEn,
    primaryMuscle = primaryMuscle.name,
    difficulty = difficulty.name,
    pattern = pattern.name,
    type = type.name,
    equipmentCsv = equipment.joinToString(",") { it.name },
    locationsCsv = locations.joinToString(",") { it.name },
    goalsCsv = goals.joinToString(",") { it.name },
    animationKey = animationKey,
    json = moshi.writeJson(this)
)

fun ExerciseEntity.toDomain(moshi: com.squareup.moshi.Moshi): Exercise? {
    val fromJson = moshi.readJson<Exercise>(json)
    if (fromJson != null) return fromJson
    return Exercise(
        id = id,
        nameEs = nameEs,
        nameEn = nameEn,
        primaryMuscle = runCatching { MuscleGroup.valueOf(primaryMuscle) }.getOrDefault(MuscleGroup.FULL_BODY),
        difficulty = runCatching { Difficulty.valueOf(difficulty) }.getOrDefault(Difficulty.BEGINNER),
        pattern = runCatching { MovementPattern.valueOf(pattern) }.getOrDefault(MovementPattern.CORE),
        type = runCatching { ExerciseType.valueOf(type) }.getOrDefault(ExerciseType.BODYWEIGHT),
        equipment = equipmentCsv.split(",").mapNotNull { runCatching { EquipmentType.valueOf(it) }.getOrNull() }.toSet(),
        locations = locationsCsv.split(",").mapNotNull { runCatching { LocationType.valueOf(it) }.getOrNull() }.toSet(),
        goals = goalsCsv.split(",").mapNotNull { runCatching { Goal.valueOf(it) }.getOrNull() }.toSet(),
        animationKey = animationKey
    )
}

fun TrainingPlan.toEntity(moshi: com.squareup.moshi.Moshi) = PlanEntity(
    id = id,
    name = name,
    status = status.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    planJson = moshi.writeJson(this)
)

fun PlanEntity.toDomain(moshi: com.squareup.moshi.Moshi): TrainingPlan? =
    moshi.readJson<TrainingPlan>(planJson)?.copy(
        id = id,
        name = name,
        status = runCatching { PlanStatus.valueOf(status) }.getOrDefault(PlanStatus.ACTIVE),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun BodyMetric.toEntity() = BodyMetricEntity(
    id = id,
    date = date.toString(),
    weightKg = weightKg,
    waistCm = waistCm,
    notes = notes
)

fun BodyMetricEntity.toDomain(): BodyMetric = BodyMetric(
    id = id,
    date = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now()),
    weightKg = weightKg,
    waistCm = waistCm,
    notes = notes
)
