package com.fitplanner.ai.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        HealthEntity::class,
        ExperienceEntity::class,
        AvailabilityEntity::class,
        EquipmentProfileEntity::class,
        ExerciseEntity::class,
        FavoriteEntity::class,
        PlanEntity::class,
        BodyMetricEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FitPlannerDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao
    abstract fun bodyMetricDao(): BodyMetricDao
}
