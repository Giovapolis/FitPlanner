package com.fitplanner.ai.core.model

import java.time.DayOfWeek
import java.time.LocalDate

data class UserProfile(
    val id: Long = 1,
    val ageRange: AgeRange? = null,
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val primaryGoal: Goal = Goal.GENERAL_FITNESS,
    val secondaryGoals: List<Goal> = emptyList(),
    val experienceLevel: ExperienceLevel = ExperienceLevel.BEGINNER,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val updatedAt: Long = System.currentTimeMillis()
)

data class HealthScreening(
    val hasInjury: Boolean? = null,
    val injuryAreas: Set<BodyArea> = emptySet(),
    val injuryNotes: String = "",
    val hasAvoidedExercises: Boolean? = null,
    val avoidedExercises: List<String> = emptyList(),
    val hasHealthCondition: Boolean? = null,
    val healthNotes: String = "",
    val hasRedFlags: Boolean? = null,
    val redFlagNotes: String = "",
    val hasMedicalRecommendation: Boolean? = null,
    val medicalNotes: String = "",
    val prefersNotToAnswer: Boolean = false
)

data class TrainingExperience(
    val monthsTraining: Int? = null,
    val weeklyFrequency: Int? = null,
    val knownExercises: List<String> = emptyList(),
    val musclePreferences: Set<MuscleGroup> = emptySet(),
    val intensityPreference: IntensityPreference = IntensityPreference.MODERATE,
    val exercisesToAvoid: List<String> = emptyList()
)

data class DayAvailability(
    val dayOfWeek: DayOfWeek,
    val available: Boolean = false,
    val preferredTime: String? = null,
    val maxMinutes: Int = 45,
    val sessionType: SessionType = SessionType.FULL_BODY,
    val allowLightActivity: Boolean = true
)

data class EquipmentProfile(
    val id: Long = 0,
    val name: String = "Casa",
    val location: LocationType = LocationType.HOME,
    val items: Set<EquipmentType> = setOf(EquipmentType.BODYWEIGHT),
    val isDefault: Boolean = true
)

data class ExerciseMistake(
    val mistake: String,
    val correction: String
)

data class Exercise(
    val id: String,
    val nameEs: String,
    val nameEn: String = "",
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: Set<MuscleGroup> = emptySet(),
    val equipment: Set<EquipmentType> = setOf(EquipmentType.BODYWEIGHT),
    val locations: Set<LocationType> = setOf(LocationType.HOME, LocationType.GYM),
    val difficulty: Difficulty = Difficulty.BEGINNER,
    val pattern: MovementPattern,
    val goals: Set<Goal> = setOf(Goal.GENERAL_FITNESS),
    val type: ExerciseType = ExerciseType.COMPOUND,
    val summary: String = "",
    val startPosition: String = "",
    val setup: String = "",
    val steps: List<String> = emptyList(),
    val breathing: String = "",
    val keyPoints: List<String> = emptyList(),
    val mistakes: List<ExerciseMistake> = emptyList(),
    val easierVariations: List<String> = emptyList(),
    val harderVariations: List<String> = emptyList(),
    val homeAlternative: String = "",
    val gymAlternative: String = "",
    val precautions: List<String> = emptyList(),
    val restrictionTags: Set<String> = emptySet(),
    val sourceUrl: String? = null,
    val animationKey: String = id,
    val isCustom: Boolean = false
)

data class SetLog(
    val id: Long = 0,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double? = null,
    val rpe: Int? = null,
    val completedAt: Long = System.currentTimeMillis()
)

data class SessionExercise(
    val id: Long = 0,
    val exerciseId: String,
    val exerciseName: String,
    val primaryMuscle: MuscleGroup,
    val orderIndex: Int,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val restSeconds: Int,
    val timeMinutes: Int? = null,
    val suggestedWeightKg: Double? = null,
    val intensityNote: String = "",
    val techniqueCue: String = "",
    val status: ExerciseStatus = ExerciseStatus.PENDING,
    val substitutedFrom: String? = null,
    val logs: List<SetLog> = emptyList()
) {
    val completedSets: Int get() = logs.size
    val isComplete: Boolean get() = logs.size >= sets
}

data class WorkoutSession(
    val id: Long = 0,
    val date: LocalDate,
    val name: String,
    val objective: String,
    val estimatedMinutes: Int,
    val location: LocationType,
    val warmup: List<String> = emptyList(),
    val exercises: List<SessionExercise> = emptyList(),
    val cooldown: List<String> = emptyList(),
    val status: SessionStatus = SessionStatus.PLANNED,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val notes: String = ""
) {
    val completedExercises: Int get() = exercises.count { it.status == ExerciseStatus.COMPLETED }
}

data class TrainingWeek(
    val id: Long = 0,
    val weekNumber: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val focus: String,
    val status: WeekStatus = WeekStatus.PLANNED,
    val sessions: List<WorkoutSession> = emptyList(),
    val review: WeekReview? = null
) {
    val plannedSessions: Int get() = sessions.count { it.status != SessionStatus.SKIPPED }
    val completedSessions: Int get() = sessions.count { it.status == SessionStatus.COMPLETED }
    val compliancePercent: Int
        get() = if (plannedSessions == 0) 0 else (completedSessions * 100) / plannedSessions
}

data class TrainingPlan(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: PlanStatus = PlanStatus.ACTIVE,
    val weeks: List<TrainingWeek> = emptyList(),
    val generatorVersion: String = "1.0"
) {
    fun activeWeek(): TrainingWeek? =
        weeks.firstOrNull { it.status == WeekStatus.ACTIVE }
            ?: weeks.firstOrNull { it.status == WeekStatus.PLANNED }
            ?: weeks.lastOrNull()
}

data class WeekReview(
    val completedSessions: Int,
    val perceivedDifficulty: Int,
    val hadPain: Boolean,
    val painNotes: String = "",
    val metDuration: Boolean,
    val adjustment: Adjustment = Adjustment.MAINTAIN,
    val createdAt: Long = System.currentTimeMillis()
)

data class BodyMetric(
    val id: Long = 0,
    val date: LocalDate,
    val weightKg: Double? = null,
    val waistCm: Double? = null,
    val notes: String = ""
)

data class PersonalRecord(
    val exerciseId: String,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val date: LocalDate
)

data class PlanGenerationInput(
    val profile: UserProfile,
    val health: HealthScreening,
    val experience: TrainingExperience,
    val availability: List<DayAvailability>,
    val equipmentProfiles: List<EquipmentProfile>,
    val exercises: List<Exercise>,
    val weekStart: LocalDate,
    val weekNumber: Int = 1,
    val previousReview: WeekReview? = null
)

sealed class GenerationResult {
    data class Success(val plan: TrainingPlan, val warnings: List<String> = emptyList()) : GenerationResult()
    data class Blocked(val reason: String, val detail: String) : GenerationResult()
    data class Error(val message: String) : GenerationResult()
}

data class WeeklyProgress(
    val week: TrainingWeek?,
    val compliancePercent: Int = 0,
    val completedSessions: Int = 0,
    val plannedSessions: Int = 0,
    val muscleVolume: Map<MuscleGroup, Int> = emptyMap(),
    val personalRecords: List<PersonalRecord> = emptyList()
)

data class DashboardData(
    val profile: UserProfile? = null,
    val activePlan: TrainingPlan? = null,
    val activeWeek: TrainingWeek? = null,
    val todaySession: WorkoutSession? = null,
    val nextSession: WorkoutSession? = null,
    val lastCompletedSession: WorkoutSession? = null,
    val weeklyCompliance: Int = 0,
    val completedThisWeek: Int = 0,
    val plannedThisWeek: Int = 0,
    val muscleVolume: Map<MuscleGroup, Int> = emptyMap()
)

data class OnboardingDraft(
    val step: Int = 0,
    val profile: UserProfile = UserProfile(),
    val health: HealthScreening = HealthScreening(),
    val experience: TrainingExperience = TrainingExperience(),
    val availability: List<DayAvailability> = DayOfWeek.values().map { DayAvailability(it) },
    val equipmentProfiles: List<EquipmentProfile> = listOf(
        EquipmentProfile(
            id = 1,
            name = "Casa",
            location = LocationType.HOME,
            items = setOf(
                EquipmentType.BODYWEIGHT,
                EquipmentType.DUMBBELLS,
                EquipmentType.MAT
            )
        ),
        EquipmentProfile(
            id = 2,
            name = "Gimnasio",
            location = LocationType.GYM,
            isDefault = false,
            items = setOf(
                EquipmentType.BODYWEIGHT,
                EquipmentType.DUMBBELLS,
                EquipmentType.BARBELL,
                EquipmentType.BENCH,
                EquipmentType.CABLE_MACHINE,
                EquipmentType.LEG_PRESS,
                EquipmentType.LEG_EXTENSION_CURL,
                EquipmentType.CHEST_PRESS_MACHINE,
                EquipmentType.LAT_PULLDOWN,
                EquipmentType.SEATED_ROW,
                EquipmentType.ADJUSTABLE_BENCH,
                EquipmentType.CABLES,
                EquipmentType.CARDIO_MACHINES
            )
        )
    ),
    val disclaimerAccepted: Boolean = false
)

data class AiSettings(
    val mode: AiMode = AiMode.OFF,
    val baseUrl: String = "https://api.openai.com/v1",
    val model: String = "gpt-4o-mini",
    val hasApiKey: Boolean = false,
    val consentAccepted: Boolean = false
)

data class AppPreferences(
    val onboardingCompleted: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val reminderTime: String = "18:00",
    val themeMode: String = "system",
    val aiMode: AiMode = AiMode.OFF,
    val aiBaseUrl: String = "https://api.openai.com/v1",
    val aiModel: String = "gpt-4o-mini",
    val aiConsentAccepted: Boolean = false
)

data class AiExerciseSummary(
    val id: String,
    val name: String,
    val muscle: String,
    val equipment: List<String>
)

data class AiSuggestionRequest(
    val goal: Goal,
    val level: ExperienceLevel,
    val daysPerWeek: Int,
    val minutesPerSession: Int,
    val equipment: List<String>,
    val split: String,
    val exerciseSlugs: List<String>,
    val restrictionTags: List<String>,
    val currentExerciseId: String,
    val catalog: List<AiExerciseSummary>
)

data class AiSuggestionResponse(
    val notes: List<String> = emptyList(),
    val exerciseSlugs: List<String> = emptyList()
)
