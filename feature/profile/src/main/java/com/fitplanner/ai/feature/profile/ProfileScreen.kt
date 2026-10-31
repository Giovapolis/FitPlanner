package com.fitplanner.ai.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitplanner.ai.core.designsystem.components.FpCard
import com.fitplanner.ai.core.designsystem.components.FpPrimaryButton
import com.fitplanner.ai.core.designsystem.components.FpSecondaryButton
import com.fitplanner.ai.core.designsystem.components.SectionHeader
import com.fitplanner.ai.core.designsystem.components.StatusPill
import com.fitplanner.ai.core.model.AiMode
import com.fitplanner.ai.core.model.BodyArea
import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.Goal
import com.fitplanner.ai.core.model.MuscleGroup
import com.fitplanner.ai.core.model.fullLabel
import java.time.format.DateTimeFormatter

@Composable
fun ProfileScreen(
    onOpenPlanner: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.showResetConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissReset,
            title = { Text("Reiniciar asistente") },
            text = { Text("Volverás al cuestionario con tus datos actuales precargados. No se borra tu historial de entrenamientos.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmReset) { Text("Reiniciar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissReset) { Text("Cancelar") }
            }
        )
    }
    if (state.showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearHistory,
            title = { Text("Borrar historial de planes") },
            text = { Text("Se eliminarán las rutinas y las series registradas. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmClearHistory) { Text("Borrar historial") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClearHistory) { Text("Cancelar") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Perfil",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tus datos, equipo y preferencias. Todo se guarda en este dispositivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (state.loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            return@LazyColumn
        }
        if (state.message != null) {
            item {
                FpCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        text = state.message!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        item { PersonalCard(state) }
        item { HealthCard(state) }
        item { EquipmentCard(state) }
        item { AvailabilityCard(state) }
        item { SettingsCard(state, viewModel) }
        item { AiCard(state, viewModel) }
        item { DataActions(viewModel, onOpenPlanner) }
        item { DisclaimerCard() }
    }
}

@Composable
private fun PersonalCard(state: ProfileUiState) {
    val profile = state.profile
    FpCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Datos personales",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatusPill(text = profile?.experienceLevel?.label ?: "Sin configurar")
            }
            InfoRow("Objetivo principal", profile?.primaryGoal?.label ?: "—")
            InfoRow("Edad", profile?.ageRange?.label ?: "No indicada")
            InfoRow("Estatura", profile?.heightCm?.let { "$it cm" } ?: "No indicada")
            InfoRow("Peso", profile?.weightKg?.let { "${formatWeight(it)} kg" } ?: "No indicado")
            InfoRow("Unidades", profile?.unitSystem?.label ?: "kg / cm")
            val secondaryGoals = profile?.secondaryGoals.orEmpty()
            if (secondaryGoals.isNotEmpty()) {
                InfoRow("Objetivos secundarios", secondaryGoals.joinToString { it.label })
            }
        }
    }
}

@Composable
private fun HealthCard(state: ProfileUiState) {
    val health = state.health
    val hasWarning = health?.hasRedFlags == true || health?.hasMedicalRecommendation == true
    FpCard(
        containerColor = if (hasWarning) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.surface
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (hasWarning) Icons.Outlined.WarningAmber else Icons.Outlined.Check,
                    contentDescription = null,
                    tint = if (hasWarning) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Salud y precauciones",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (hasWarning) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            if (health?.hasRedFlags == true) {
                Text(
                    text = "Has indicado síntomas de alarma. No generaremos rutinas automáticas hasta que un profesional valore tu situación.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            if (health?.hasMedicalRecommendation == true) {
                Text(
                    text = "Hay una recomendación médica que respetar. La generación automática está bloqueada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            if (health?.hasInjury == true) {
                InfoRow("Lesión o limitación", health.injuryAreas.joinToString { it.label }.ifBlank { "Sí" })
                if (health.injuryNotes.isNotBlank()) InfoRow("Notas", health.injuryNotes)
            } else if (health?.hasInjury == false) {
                InfoRow("Lesión o limitación", "No declarada")
            }
            if (health?.prefersNotToAnswer == true) {
                InfoRow("Cribado", "Prefiere no responder")
            }
            if (!hasWarning && health?.hasInjury != true) {
                Text(
                    text = "Revisa esta información si cambia. El plan se adapta a lo que declaras, sin emitir diagnósticos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EquipmentCard(state: ProfileUiState) {
    FpCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Equipamiento",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (state.equipmentProfiles.isEmpty()) {
                Text(
                    text = "No hay perfiles de equipamiento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.equipmentProfiles.forEach { profile ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${profile.items.size} activos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = profile.items.take(8).joinToString(" · ") { it.label } +
                                if (profile.items.size > 8) " …" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailabilityCard(state: ProfileUiState) {
    FpCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Disponibilidad semanal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val available = state.availability.filter { it.available }
            if (available.isEmpty()) {
                Text(
                    text = "Sin días configurados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                available.forEach { day ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = day.dayOfWeek.fullLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${day.maxMinutes} min · ${day.sessionType.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(state: ProfileUiState, viewModel: ProfileViewModel) {
    var reminderTime by remember(state.preferences.reminderTime) {
        mutableStateOf(state.preferences.reminderTime)
    }
    FpCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Recordatorios",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Avisarme antes de entrenar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = state.preferences.notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled
                )
            }
            OutlinedTextField(
                value = reminderTime,
                onValueChange = { value ->
                    reminderTime = value
                    if (value.matches(Regex("\\d{2}:\\d{2}"))) viewModel.setReminderTime(value)
                },
                label = { Text("Hora preferida (HH:mm)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Los recordatorios usan WorkManager y no requieren conexión.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AiCard(state: ProfileUiState, viewModel: ProfileViewModel) {
    var enabled by remember(state.aiSettings.mode) { mutableStateOf(state.aiSettings.mode == AiMode.OPENAI_COMPATIBLE) }
    var baseUrl by remember(state.aiSettings.baseUrl) { mutableStateOf(state.aiSettings.baseUrl) }
    var model by remember(state.aiSettings.model) { mutableStateOf(state.aiSettings.model) }
    var apiKey by remember { mutableStateOf("") }
    var consent by remember(state.aiSettings.consentAccepted) { mutableStateOf(state.aiSettings.consentAccepted) }
    FpCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sugerencias con IA (opcional)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Desactivado por defecto. La app funciona sin conexión.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
            if (enabled) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("URL base compatible OpenAI") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Modelo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(if (state.aiSettings.hasApiKey) "Nueva clave (opcional)" else "API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(checked = consent, onCheckedChange = { consent = it })
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Consiento enviar un resumen anonimizado (objetivo, nivel, días, minutos, equipo y ejercicios). Nunca se envían nombre, edad exacta, peso ni notas médicas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.aiSettings.hasApiKey) {
                    Text(
                        text = "Hay una clave guardada cifrada en este dispositivo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                FpPrimaryButton(
                    text = "Guardar configuración de IA",
                    onClick = {
                        viewModel.saveAiSettings(
                            mode = if (enabled) AiMode.OPENAI_COMPATIBLE else AiMode.OFF,
                            baseUrl = baseUrl,
                            model = model,
                            apiKey = apiKey,
                            consent = consent
                        )
                        apiKey = ""
                    }
                )
                if (state.aiSettings.hasApiKey) {
                    FpSecondaryButton(
                        text = "Eliminar clave guardada",
                        onClick = viewModel::clearApiKey
                    )
                }
                Text(
                    text = "Toda sugerencia remota pasa por el validador local de equipamiento, restricciones y duración antes de mostrarse.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DataActions(viewModel: ProfileViewModel, onOpenPlanner: () -> Unit) {
    FpCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Datos y plan",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            FpSecondaryButton(
                text = "Reiniciar asistente de perfil",
                onClick = viewModel::requestReset,
                icon = Icons.Outlined.Refresh
            )
            FpSecondaryButton(
                text = "Borrar historial de planes",
                onClick = viewModel::requestClearHistory,
                icon = Icons.Outlined.Delete
            )
            FpPrimaryButton(
                text = "Ir al planificador",
                onClick = onOpenPlanner
            )
        }
    }
}

@Composable
private fun DisclaimerCard() {
    FpCard(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "FitPlanner AI es una herramienta educativa. No sustituye a un médico, fisioterapeuta o entrenador. Si aparece dolor, mareo o un síntoma de alarma, detente y consulta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatWeight(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
