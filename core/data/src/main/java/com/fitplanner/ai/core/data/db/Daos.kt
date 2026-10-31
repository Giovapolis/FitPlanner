package com.fitplanner.ai.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observeProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun getProfile(): ProfileEntity?

    @Upsert
    suspend fun upsertProfile(entity: ProfileEntity)

    @Query("SELECT * FROM health WHERE id = 1")
    fun observeHealth(): Flow<HealthEntity?>

    @Query("SELECT * FROM health WHERE id = 1")
    suspend fun getHealth(): HealthEntity?

    @Upsert
    suspend fun upsertHealth(entity: HealthEntity)

    @Query("SELECT * FROM experience WHERE id = 1")
    fun observeExperience(): Flow<ExperienceEntity?>

    @Query("SELECT * FROM experience WHERE id = 1")
    suspend fun getExperience(): ExperienceEntity?

    @Upsert
    suspend fun upsertExperience(entity: ExperienceEntity)

    @Query("SELECT * FROM availability ORDER BY dayOfWeek")
    fun observeAvailability(): Flow<List<AvailabilityEntity>>

    @Query("SELECT * FROM availability ORDER BY dayOfWeek")
    suspend fun getAvailability(): List<AvailabilityEntity>

    @Upsert
    suspend fun upsertAvailability(entities: List<AvailabilityEntity>)

    @Query("SELECT * FROM equipment_profile ORDER BY id")
    fun observeEquipmentProfiles(): Flow<List<EquipmentProfileEntity>>

    @Query("SELECT * FROM equipment_profile ORDER BY id")
    suspend fun getEquipmentProfiles(): List<EquipmentProfileEntity>

    @Upsert
    suspend fun upsertEquipmentProfiles(entities: List<EquipmentProfileEntity>)

    @Query("DELETE FROM equipment_profile WHERE id = :id")
    suspend fun deleteEquipmentProfile(id: Long)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise ORDER BY nameEs")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise ORDER BY nameEs")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(entities: List<ExerciseEntity>)

    @Query("SELECT exerciseId FROM favorite")
    fun observeFavorites(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE exerciseId = :id")
    suspend fun removeFavorite(id: String)
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plan ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plan WHERE status = 'ACTIVE' ORDER BY updatedAt DESC LIMIT 1")
    fun observeActive(): Flow<PlanEntity?>

    @Query("SELECT * FROM plan WHERE status = 'ACTIVE' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActive(): PlanEntity?

    @Query("SELECT * FROM plan WHERE id = :id")
    suspend fun getById(id: Long): PlanEntity?

    @Upsert
    suspend fun upsert(entity: PlanEntity)

    @Query("UPDATE plan SET status = 'ARCHIVED' WHERE status = 'ACTIVE'")
    suspend fun archiveActive()

    @Query("DELETE FROM plan")
    suspend fun clear()
}

@Dao
interface BodyMetricDao {
    @Query("SELECT * FROM body_metric ORDER BY date DESC")
    fun observeAll(): Flow<List<BodyMetricEntity>>

    @Upsert
    suspend fun upsert(entity: BodyMetricEntity)
}
