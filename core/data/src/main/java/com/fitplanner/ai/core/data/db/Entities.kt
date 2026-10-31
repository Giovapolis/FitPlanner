package com.fitplanner.ai.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val json: String,
    val updatedAt: Long
)

@Entity(tableName = "health")
data class HealthEntity(
    @PrimaryKey val id: Int = 1,
    val json: String,
    val updatedAt: Long
)

@Entity(tableName = "experience")
data class ExperienceEntity(
    @PrimaryKey val id: Int = 1,
    val json: String,
    val updatedAt: Long
)

@Entity(tableName = "availability")
data class AvailabilityEntity(
    @PrimaryKey val dayOfWeek: Int,
    val available: Boolean,
    val preferredTime: String?,
    val maxMinutes: Int,
    val sessionType: String,
    val allowLightActivity: Boolean
)

@Entity(tableName = "equipment_profile")
data class EquipmentProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val location: String,
    val itemsJson: String,
    val isDefault: Boolean
)

@Entity(tableName = "exercise")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val nameEs: String,
    val nameEn: String,
    val primaryMuscle: String,
    val difficulty: String,
    val pattern: String,
    val type: String,
    val equipmentCsv: String,
    val locationsCsv: String,
    val goalsCsv: String,
    val animationKey: String,
    val json: String
)

@Entity(tableName = "favorite")
data class FavoriteEntity(
    @PrimaryKey val exerciseId: String
)

@Entity(tableName = "plan")
data class PlanEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val planJson: String
)

@Entity(tableName = "body_metric")
data class BodyMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val weightKg: Double?,
    val waistCm: Double?,
    val notes: String
)
