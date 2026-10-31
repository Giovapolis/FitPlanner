package com.fitplanner.ai.core.data.ai

import com.fitplanner.ai.core.data.repository.ExerciseRepository
import com.fitplanner.ai.core.data.repository.ProfileRepository
import com.fitplanner.ai.core.data.settings.SettingsRepository
import com.fitplanner.ai.core.domain.AiSuggestionProvider
import com.fitplanner.ai.core.domain.PlanValidator
import com.fitplanner.ai.core.domain.SafetyGate
import com.fitplanner.ai.core.model.AiExerciseSummary
import com.fitplanner.ai.core.model.AiMode
import com.fitplanner.ai.core.model.AiSuggestionRequest
import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.Exercise
import com.fitplanner.ai.core.model.HealthScreening
import com.fitplanner.ai.core.model.PlanGenerationInput
import com.fitplanner.ai.core.model.SessionExercise
import com.fitplanner.ai.core.model.TrainingExperience
import com.fitplanner.ai.core.model.WorkoutSession
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class AiValidatedSuggestions(
    val exercises: List<Exercise>,
    val notes: List<String>,
    val discardedCount: Int
)

@Singleton
class AiRepository @Inject constructor(
    private val provider: AiSuggestionProvider,
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val exerciseRepository: ExerciseRepository
) {
    suspend fun suggestAlternatives(
        current: SessionExercise,
        session: WorkoutSession
    ): Result<AiValidatedSuggestions> {
        val settings = settingsRepository.aiSettings()
        if (settings.mode != AiMode.OPENAI_COMPATIBLE) {
            return Result.failure(IllegalStateException("Activa las sugerencias con IA en Perfil."))
        }
        if (!settings.consentAccepted) {
            return Result.failure(IllegalStateException("Acepta el consentimiento de envío anonimizado en Perfil."))
        }
        val apiKey = settingsRepository.readApiKey()
            ?: return Result.failure(IllegalStateException("Configura una API key en Perfil."))
        val profile = profileRepository.profile.first()
            ?: return Result.failure(IllegalStateException("Completa tu perfil antes de usar la IA."))
        val health = profileRepository.health.first() ?: HealthScreening()
        val experience = profileRepository.experience.first() ?: TrainingExperience()
        val availability = profileRepository.availability.first()
        val equipmentProfiles = profileRepository.equipmentProfiles.first()
        val profileEquipment = equipmentProfiles.firstOrNull { it.location == session.location }
        val availableEquipment = (profileEquipment?.items ?: emptySet()) + EquipmentType.BODYWEIGHT
        val restrictionTags = SafetyGate.restrictionTags(health)
        val exercises = exerciseRepository.getAll()
        val exclude = session.exercises.map { it.exerciseId }.toSet()
        val catalog = exercises
            .filter { exercise ->
                exercise.locations.contains(session.location) &&
                    exercise.equipment.all { it in availableEquipment } &&
                    exercise.restrictionTags.intersect(restrictionTags).isEmpty() &&
                    exercise.difficulty.ordinal <= profile.experienceLevel.ordinal &&
                    exercise.id !in exclude
            }
            .map { exercise ->
                AiExerciseSummary(
                    id = exercise.id,
                    name = exercise.nameEs,
                    muscle = exercise.primaryMuscle.label,
                    equipment = exercise.equipment.map { it.label }
                )
            }
        val input = PlanGenerationInput(
            profile = profile,
            health = health,
            experience = experience,
            availability = availability,
            equipmentProfiles = equipmentProfiles,
            exercises = exercises,
            weekStart = LocalDate.now(),
            weekNumber = 1
        )
        val request = AiSuggestionRequest(
            goal = profile.primaryGoal,
            level = profile.experienceLevel,
            daysPerWeek = availability.count { it.available },
            minutesPerSession = availability.filter { it.available }.maxOfOrNull { it.maxMinutes } ?: session.estimatedMinutes,
            equipment = availableEquipment.map { it.label }.sorted(),
            split = session.name,
            exerciseSlugs = session.exercises.map { it.exerciseId },
            restrictionTags = restrictionTags.toList(),
            currentExerciseId = current.exerciseId,
            catalog = catalog
        )
        return provider.suggest(request, settings, apiKey).mapCatching { response ->
            val validSlugs = PlanValidator.validateSuggestion(
                slugs = response.exerciseSlugs,
                input = input,
                sessionLocation = session.location,
                currentExerciseIds = exclude
            )
            val validExercises = validSlugs.mapNotNull { slug -> exercises.firstOrNull { it.id == slug } }
            AiValidatedSuggestions(
                exercises = validExercises,
                notes = response.notes,
                discardedCount = response.exerciseSlugs.size - validExercises.size
            )
        }
    }
}
