package com.fitplanner.ai.feature.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitplanner.ai.core.designsystem.components.EmptyState
import com.fitplanner.ai.core.designsystem.components.FpCard
import com.fitplanner.ai.core.designsystem.components.FpPrimaryButton
import com.fitplanner.ai.core.designsystem.components.FpSecondaryButton
import com.fitplanner.ai.core.designsystem.components.StatusPill
import com.fitplanner.ai.core.model.Adjustment
import com.fitplanner.ai.core.model.TrainingWeek
import com.fitplanner.ai.core.model.WeeklyProgress
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProgressScreen(
    onOpenPlanner: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.showReview) {
        val week = state.weekly.week
        if (week != null) {
            ReviewDialog(
                week = week,
                onDismiss = viewModel::closeReview,
                onSubmit = viewModel::submitReview
            )
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Progreso",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Datos reales de tus sesiones. Sin métricas inventadas.",
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
        if (state.plan == null) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Insights,
                    title = "Tu progreso aparecerá aquí",
                    message = "Genera una rutina, completa tu primera sesión y registra tus series. Entonces verás cumplimiento, volumen y récords reales.",
                    action = { FpPrimaryButton(text = "Ir al planificador", onClick = onOpenPlanner) }
                )
            }
        } else {
            item { WeekProgressCard(state.weekly) }
            item {
                FpPrimaryButton(
                    text = "Revisar la semana",
                    onClick = viewModel::openReview,
                    enabled = state.weekly.week != null
                )
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
            if (state.error != null) {
                item {
                    Text(
                        text = state.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            item { VolumeCard(state.weekly) }
            item { RecordsCard(state) }
            item { HistoryCard(state.history) }
        }
    }
}

@Composable
private fun WeekProgressCard(weekly: WeeklyProgress) {
    val week = weekly.week
    FpCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Semana ${week?.weekNumber ?: 1}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = week?.let { "${formatDate(it.startDate)} — ${formatDate(it.endDate)}" }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(text = "${weekly.compliancePercent}%")
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Stat("${weekly.completedSessions}", "Completadas")
            Stat("${weekly.plannedSessions}", "Planificadas")
            Stat("${weekly.muscleVolume.values.sum()}", "Series")
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(weekly.compliancePercent / 100f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.secondary)
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VolumeCard(weekly: WeeklyProgress) {
    FpCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Volumen por grupo muscular",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (weekly.muscleVolume.isEmpty()) {
                Text(
                    text = "Completa series para ver el volumen semanal. Solo contamos series registradas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val max = weekly.muscleVolume.values.maxOrNull() ?: 1
                weekly.muscleVolume.entries.sortedByDescending { it.value }.forEach { (muscle, sets) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = muscle.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(92.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((sets.toFloat() / max).coerceIn(0.05f, 1f))
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$sets",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordsCard(state: ProgressUiState) {
    FpCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Récords personales",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (state.records.isEmpty()) {
                Text(
                    text = "Aún no hay pesos registrados. Cuando anotes una carga, aparecerá aquí.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.records.take(5).forEach { record ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.exerciseName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${record.reps} reps · ${formatDate(record.date)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${formatWeight(record.weightKg)} kg",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(history: List<TrainingWeek>) {
    FpCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Historial de semanas",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (history.isEmpty()) {
                Text(
                    text = "Las semanas completadas se guardarán aquí sin sobrescribir el pasado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                history.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Semana ${week.weekNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${formatDate(week.startDate)} — ${formatDate(week.endDate)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            text = "${week.compliancePercent}%",
                            color = if (week.compliancePercent >= 60) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewDialog(
    week: TrainingWeek,
    onDismiss: () -> Unit,
    onSubmit: (Int, Int, Boolean, String, Boolean, Adjustment) -> Unit
) {
    var completed by remember { mutableIntStateOf(week.completedSessions) }
    var difficulty by remember { mutableIntStateOf(3) }
    var hadPain by remember { mutableStateOf(false) }
    var painNotes by remember { mutableStateOf("") }
    var metDuration by remember { mutableStateOf(true) }
    var adjustment by remember { mutableStateOf(Adjustment.MAINTAIN) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Revisión de la semana ${week.weekNumber}") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 430.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = completed.toString(),
                        onValueChange = { text ->
                            if (text.all { it.isDigit() }) completed = text.toIntOrNull() ?: 0
                        },
                        label = { Text("Entrenamientos completados") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text(
                        text = "¿Cómo de difícil fue?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items((1..5).toList()) { value ->
                            FilterChip(
                                selected = difficulty == value,
                                onClick = { difficulty = value },
                                label = { Text("$value") }
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿Tuviste molestias o dolor?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(checked = hadPain, onCheckedChange = { hadPain = it })
                    }
                    if (hadPain) {
                        OutlinedTextField(
                            value = painNotes,
                            onValueChange = { painNotes = it },
                            label = { Text("¿Dónde y cuándo? (opcional)") },
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            minLines = 2
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿Pudiste cumplir la duración?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(checked = metDuration, onCheckedChange = { metDuration = it })
                    }
                }
                item {
                    Text(
                        text = "Ajuste para la próxima semana",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Adjustment.values().forEach { option ->
                            FilterChip(
                                selected = adjustment == option,
                                onClick = { adjustment = option },
                                label = {
                                    Text(
                                        when (option) {
                                            Adjustment.MAINTAIN -> "Mantener"
                                            Adjustment.REDUCE -> "Reducir"
                                            Adjustment.INCREASE -> "Aumentar poco"
                                        }
                                    )
                                }
                            )
                        }
                    }
                    if (hadPain) {
                        Text(
                            text = "Con molestias, el motor reducirá el volumen y no subirá cargas.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSubmit(completed, difficulty, hadPain, painNotes, metDuration, adjustment)
                }
            ) { Text("Guardar revisión") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun formatDate(date: java.time.LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-ES")))

private fun formatWeight(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
