package com.fitplanner.ai.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.fitplanner.ai.core.data.db.BodyMetricDao
import com.fitplanner.ai.core.data.db.ExerciseDao
import com.fitplanner.ai.core.data.db.FitPlannerDatabase
import com.fitplanner.ai.core.data.db.PlanDao
import com.fitplanner.ai.core.data.ai.OpenAiCompatibleProvider
import com.fitplanner.ai.core.data.db.ProfileDao
import com.fitplanner.ai.core.domain.AiSuggestionProvider
import com.squareup.moshi.Moshi
import com.fitplanner.ai.core.data.AppMoshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.fitPlannerDataStore by preferencesDataStore(name = "fitplanner_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FitPlannerDatabase =
        Room.databaseBuilder(context, FitPlannerDatabase::class.java, "fitplanner.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideProfileDao(db: FitPlannerDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideExerciseDao(db: FitPlannerDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun providePlanDao(db: FitPlannerDatabase): PlanDao = db.planDao()

    @Provides
    fun provideBodyMetricDao(db: FitPlannerDatabase): BodyMetricDao = db.bodyMetricDao()

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = AppMoshi

    @Provides
    @Singleton
    fun provideAiSuggestionProvider(provider: OpenAiCompatibleProvider): AiSuggestionProvider = provider

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.fitPlannerDataStore
}
