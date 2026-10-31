package com.fitplanner.ai.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitplanner.ai.core.designsystem.components.FpCard
import com.fitplanner.ai.core.designsystem.components.FpPrimaryButton
import com.fitplanner.ai.core.designsystem.components.FpSecondaryButton
import com.fitplanner.ai.core.designsystem.components.SectionHeader
import com.fitplanner.ai.core.designsystem.components.StatusPill
import com.fitplanner.ai.core.model.AgeRange
import com.fitplanner.ai.core.model.ActivityLevel
import com.fitplanner.ai.core.model.BodyArea
import com.fitplanner.ai.core.model.DayAvailability
import com.fitplanner.ai.core.model.EquipmentProfile
import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.ExperienceLevel
import com.fitplanner.ai.core.model.Goal
import com.fitplanner.ai.core.model.IntensityPreference
import com.fitplanner.ai.core.model.LocationType
import com.fitplanner.ai.core.model.MuscleGroup
import com.fitplanner.ai.core.model.SessionType
import com.fitplanner.ai.core.model.UnitSystem
import com.fitplanner.ai.core.model.fullLabel
import com.fitplanner.ai.core.model.shortLabel

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.completed) {
        if (state.completed) onComplete()
    }
    if (state.safetyWarning != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissWarning,
            icon = { Icon(Icons.Outlined.WarningAmber, contentDescription = null) },
            title = { Text(state.safetyWarning!!.reason) },
            text = { Text(state.safetyWarning!!.detail) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmWarningAndFinish) {
                    Text("Entiendo y continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissWarning) {
                    Text("Revisar respuestas")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FitPlanner AI",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tu plan, paso a paso",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                StatusPill(text = "Paso ${state.step + 1} de ${state.totalSteps}")
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(state.totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                color = if (index <= state.step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(99.dp)
                            )
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (state.step) {
                    0 -> PersonalStep(state, viewModel)
                    1 -> HealthStep(state, viewModel)
                    2 -> ExperienceStep(state, viewModel)
                    3 -> AvailabilityStep(state, viewModel)
                    else -> EquipmentStep(state, viewModel)
                }
                Spacer(Modifier.height(22.dp))
            }
            if (state.error != null) {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.step > 0) {
                    OutlinedButton(
                        onClick = viewModel::back,
                        modifier = Modifier.weight(0.4f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Atrás")
                    }
                }
                Box(modifier = Modifier.weight(0.6f)) {
                    FpPrimaryButton(
                        text = if (state.step == state.totalSteps - 1) "Crear mi plan" else "Siguiente",
                        onClick = {
                            if (state.step == state.totalSteps - 1) viewModel.finish() else viewModel.next()
                        },
                        enabled = !state.saving
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val profile = state.draft.profile
    StepHeader(
        title = "Empecemos por ti",
        subtitle = "Estos datos solo ajustan el plan. Puedes omitir lo que no quieras compartir."
    )
    FpCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DropdownField(
                label = "Rango de edad",
                selected = profile.ageRange?.label ?: "Prefiero no indicarlo",
                options = listOf("Prefiero no indicarlo") + OnboardingViewModel.ageRanges.map { it.label },
                onSelect = { value ->
                    viewModel.updateProfile {
                        it.copy(ageRange = OnboardingViewModel.ageRanges.firstOrNull { age -> age.label == value })
                    }
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(
                    label = "Estatura (cm)",
                    value = profile.heightCm?.toString().orEmpty(),
                    modifier = Modifier.weight(1f),
                    onValueChange = { viewModel.updateProfile { p -> p.copy(heightCm = it.toIntOrNull()) } }
                )
                NumberField(
                    label = "Peso (kg, opcional)",
                    value = profile.weightKg?.toString().orEmpty(),
                    modifier = Modifier.weight(1f),
                    onValueChange = { viewModel.updateProfile { p -> p.copy(weightKg = it.toDoubleOrNull()) } }
                )
            }
            DropdownField(
                label = "Unidades",
                selected = profile.unitSystem.label,
                options = UnitSystem.values().map { it.label },
                onSelect = { value ->
                    viewModel.updateProfile { it.copy(unitSystem = UnitSystem.values().first { u -> u.label == value }) }
                }
            )
            DropdownField(
                label = "Actividad física habitual",
                selected = profile.activityLevel.label,
                options = ActivityLevel.values().map { it.label },
                onSelect = { value ->
                    viewModel.updateProfile {
                        it.copy(activityLevel = ActivityLevel.values().first { a -> a.label == value })
                    }
                }
            )
            InfoNote("La edad, estatura y peso ayudan a orientar el volumen y las progresiones. No son obligatorios para empezar.")
        }
    }
    Spacer(Modifier.height(20.dp))
    SectionHeader(title = "Objetivo principal", subtitle = "Elige el que más te importe ahora")
    Spacer(Modifier.height(10.dp))
    Goal.values().forEach { goal ->
        SelectableRow(
            title = goal.label,
            selected = profile.primaryGoal == goal,
            onClick = { viewModel.updateProfile { it.copy(primaryGoal = goal) } }
        )
        Spacer(Modifier.height(8.dp))
    }
    Spacer(Modifier.height(12.dp))
    SectionHeader(title = "Objetivos secundarios", subtitle = "Opcional")
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Goal.values().toList()) { goal ->
            FilterChip(
                selected = goal in profile.secondaryGoals,
                onClick = {
                    viewModel.updateProfile {
                        it.copy(
                            secondaryGoals = if (goal in it.secondaryGoals) it.secondaryGoals - goal
                            else it.secondaryGoals + goal
                        )
                    }
                },
                label = { Text(goal.label) }
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    SectionHeader(title = "Nivel de experiencia")
    Spacer(Modifier.height(8.dp))
    SegmentedRow(
        options = ExperienceLevel.values().toList(),
        selected = profile.experienceLevel,
        label = { it.label },
        onSelect = { level -> viewModel.updateProfile { it.copy(experienceLevel = level) } }
    )
}

@Composable
private fun HealthStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val health = state.draft.health
    StepHeader(
        title = "Muévete con precaución",
        subtitle = "No hacemos diagnósticos. Solo adaptamos el plan a lo que nos cuentes."
    )
    FpCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Si tienes dolor, mareos, desmayos, dolor de pecho o dificultad respiratoria inusual al entrenar, consulta con un profesional antes de empezar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    YesNoQuestion(
        question = "¿Tienes alguna lesión actual o limitación de movimiento?",
        value = health.hasInjury,
        onValue = { viewModel.updateHealth { h -> h.copy(hasInjury = it) } }
    )
    if (health.hasInjury == true) {
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BodyArea.values().toList()) { area ->
                FilterChip(
                    selected = area in health.injuryAreas,
                    onClick = {
                        viewModel.updateHealth {
                            it.copy(
                                injuryAreas = if (area in it.injuryAreas) it.injuryAreas - area else it.injuryAreas + area
                            )
                        }
                    },
                    label = { Text(area.label) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = health.injuryNotes,
            onValueChange = { value -> viewModel.updateHealth { it.copy(injuryNotes = value) } },
            label = { Text("Notas breves (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
    }
    Spacer(Modifier.height(14.dp))
    YesNoQuestion(
        question = "¿Hay ejercicios que te hayan indicado evitar?",
        value = health.hasAvoidedExercises,
        onValue = { viewModel.updateHealth { h -> h.copy(hasAvoidedExercises = it) } }
    )
    if (health.hasAvoidedExercises == true) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = health.avoidedExercises.joinToString(", "),
            onValueChange = { value ->
                viewModel.updateHealth {
                    it.copy(avoidedExercises = value.split(",").map(String::trim).filter(String::isNotEmpty))
                }
            },
            label = { Text("Ejercicios a evitar, separados por comas") },
            modifier = Modifier.fillMaxWidth()
        )
    }
    Spacer(Modifier.height(14.dp))
    YesNoQuestion(
        question = "¿Tienes alguna condición de salud que pueda afectar a la actividad física?",
        value = health.hasHealthCondition,
        onValue = { viewModel.updateHealth { h -> h.copy(hasHealthCondition = it) } }
    )
    if (health.hasHealthCondition == true) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = health.healthNotes,
            onValueChange = { value -> viewModel.updateHealth { it.copy(healthNotes = value) } },
            label = { Text("Notas (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
    }
    Spacer(Modifier.height(14.dp))
    YesNoQuestion(
        question = "¿Experimentas dolor, mareos, desmayos, dolor de pecho o dificultad respiratoria inusual durante el ejercicio?",
        value = health.hasRedFlags,
        onValue = { viewModel.updateHealth { h -> h.copy(hasRedFlags = it) } }
    )
    if (health.hasRedFlags == true) {
        Spacer(Modifier.height(8.dp))
        WarningNote("Esto es una señal de alarma. No generaremos una rutina automática; busca valoración profesional.")
    }
    Spacer(Modifier.height(14.dp))
    YesNoQuestion(
        question = "¿Tienes alguna recomendación médica que debamos respetar?",
        value = health.hasMedicalRecommendation,
        onValue = { viewModel.updateHealth { h -> h.copy(hasMedicalRecommendation = it) } }
    )
    Spacer(Modifier.height(14.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.updateHealth { h -> h.copy(prefersNotToAnswer = !h.prefersNotToAnswer) } }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = health.prefersNotToAnswer,
            onCheckedChange = { value -> viewModel.updateHealth { h -> h.copy(prefersNotToAnswer = value) } }
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Prefiero no responder a estas preguntas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ExperienceStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val experience = state.draft.experience
    StepHeader(
        title = "Tu punto de partida",
        subtitle = "No asumimos que más experiencia signifique más volumen. Ajustaremos la progresión."
    )
    FpCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(
                    label = "Meses entrenando",
                    value = experience.monthsTraining?.toString().orEmpty(),
                    modifier = Modifier.weight(1f),
                    onValueChange = { viewModel.updateExperience { e -> e.copy(monthsTraining = it.toIntOrNull()) } }
                )
                NumberField(
                    label = "Días por semana",
                    value = experience.weeklyFrequency?.toString().orEmpty(),
                    modifier = Modifier.weight(1f),
                    onValueChange = { viewModel.updateExperience { e -> e.copy(weeklyFrequency = it.toIntOrNull()) } }
                )
            }
            OutlinedTextField(
                value = experience.knownExercises.joinToString(", "),
                onValueChange = { value ->
                    viewModel.updateExperience {
                        it.copy(knownExercises = value.split(",").map(String::trim).filter(String::isNotEmpty))
                    }
                },
                label = { Text("Ejercicios que conoces (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = experience.exercisesToAvoid.joinToString(", "),
                onValueChange = { value ->
                    viewModel.updateExperience {
                        it.copy(exercisesToAvoid = value.split(",").map(String::trim).filter(String::isNotEmpty))
                    }
                },
                label = { Text("Ejercicios que deseas evitar") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    Spacer(Modifier.height(18.dp))
    SectionHeader(title = "Preferencias musculares", subtitle = "Opcional")
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(MuscleGroup.values().toList()) { muscle ->
            FilterChip(
                selected = muscle in experience.musclePreferences,
                onClick = {
                    viewModel.updateExperience {
                        it.copy(
                            musclePreferences = if (muscle in it.musclePreferences)
                                it.musclePreferences - muscle else it.musclePreferences + muscle
                        )
                    }
                },
                label = { Text(muscle.label) }
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    SectionHeader(title = "Intensidad preferida")
    Spacer(Modifier.height(8.dp))
    SegmentedRow(
        options = IntensityPreference.values().toList(),
        selected = experience.intensityPreference,
        label = { it.label },
        onSelect = { value -> viewModel.updateExperience { it.copy(intensityPreference = value) } }
    )
    Spacer(Modifier.height(14.dp))
    InfoNote("Si eliges una intensidad exigente, el motor seguirá respetando las restricciones y el tiempo disponible.")
}

@Composable
private fun AvailabilityStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    StepHeader(
        title = "Tu semana real",
        subtitle = "Elige días y minutos. El calentamiento, los descansos y la vuelta a la calma cuentan dentro del tiempo."
    )
    state.draft.availability.forEachIndexed { index, day ->
        AvailabilityCard(
            day = day,
            onChange = { transform -> viewModel.updateAvailability(index, transform) }
        )
        Spacer(Modifier.height(10.dp))
    }
    InfoNote("Puedes marcar un día como recuperación activa. El plan no llenará todos los huecos si no es necesario.")
}

@Composable
private fun EquipmentStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    StepHeader(
        title = "Dónde entrenas",
        subtitle = "Solo usaremos ejercicios con el equipamiento que tengas disponible."
    )
    state.draft.equipmentProfiles.forEach { profile ->
        EquipmentProfileCard(profile = profile, viewModel = viewModel)
        Spacer(Modifier.height(14.dp))
    }
    FpCard {
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = state.draft.disclaimerAccepted,
                onCheckedChange = viewModel::setDisclaimerAccepted
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Entiendo que FitPlanner AI es una herramienta educativa y no sustituye la valoración de un médico, fisioterapeuta o entrenador. Si aparece dolor o un síntoma de alarma, debo detenerme y consultar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentProfileCard(profile: EquipmentProfile, viewModel: OnboardingViewModel) {
    val availableItems = EquipmentType.values().filter {
        it.location == LocationType.BOTH || it.location == profile.location
    }
    FpCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${profile.items.size} elementos activos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(text = profile.location.label)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableItems.forEach { equipment ->
                    FilterChip(
                        selected = equipment in profile.items,
                        onClick = { viewModel.toggleEquipment(profile.location, equipment) },
                        label = { Text(equipment.label) }
                    )
                }
            }
            if (profile.items.isEmpty()) {
                WarningNote("Activa al menos el peso corporal o el equipamiento que tengas.")
            }
        }
    }
}

@Composable
private fun AvailabilityCard(
    day: DayAvailability,
    onChange: ((DayAvailability) -> DayAvailability) -> Unit
) {
    FpCard(
        borderColor = if (day.available) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = day.dayOfWeek.fullLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = day.dayOfWeek.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = day.available,
                    onCheckedChange = { value -> onChange { it.copy(available = value) } }
                )
            }
            if (day.available) {
                Text(
                    text = "Tiempo máximo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(15, 30, 45, 60, 75, 90)) { minutes ->
                        FilterChip(
                            selected = day.maxMinutes == minutes,
                            onClick = { onChange { it.copy(maxMinutes = minutes) } },
                            label = { Text("$minutes min") }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        DropdownField(
                            label = "Tipo de sesión",
                            selected = day.sessionType.label,
                            options = SessionType.values().map { it.label },
                            onSelect = { value ->
                                onChange {
                                    it.copy(sessionType = SessionType.values().first { s -> s.label == value })
                                }
                            }
                        )
                    }
                    Box(modifier = Modifier.weight(0.7f)) {
                        OutlinedTextField(
                            value = day.preferredTime.orEmpty(),
                            onValueChange = { value -> onChange { it.copy(preferredTime = value.ifBlank { null }) } },
                            label = { Text("Hora") },
                            placeholder = { Text("18:00") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Permitir actividad ligera",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = day.allowLightActivity,
                        onCheckedChange = { value -> onChange { it.copy(allowLightActivity = value) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun DropdownField(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Text(selected, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text("▾", style = MaterialTheme.typography.bodyMedium)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> if (text.all { it.isDigit() || it == '.' }) onValueChange(text) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun SelectableRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FpCard(
        modifier = Modifier.clickable(onClick = onClick),
        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        borderColor = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        shape = RoundedCornerShape(99.dp)
                    )
                    .then(
                        if (!selected) Modifier.background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(99.dp)
                        ) else Modifier
                    )
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun <T> SegmentedRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            OutlinedButton(
                onClick = { onSelect(option) },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun YesNoQuestion(
    question: String,
    value: Boolean?,
    onValue: (Boolean) -> Unit
) {
    Column {
        Text(
            text = question,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = value == true,
                onClick = { onValue(true) },
                label = { Text("Sí") }
            )
            FilterChip(
                selected = value == false,
                onClick = { onValue(false) },
                label = { Text("No") }
            )
        }
    }
}

@Composable
private fun InfoNote(text: String) {
    FpCard(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun WarningNote(text: String) {
    FpCard(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
