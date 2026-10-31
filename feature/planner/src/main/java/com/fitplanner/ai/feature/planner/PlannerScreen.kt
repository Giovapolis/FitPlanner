package com.fitplanner.ai.feature.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitplanner.ai.core.designsystem.components.EmptyState
import com.fitplanner.ai.core.designsystem.components.FpCard
import com.fitplanner.ai.core.designsystem.components.FpPrimaryButton
import com.fitplanner.ai.core.designsystem.components.SectionHeader
import com.fitplanner.ai.core.designsystem.components.StatusPill
import com.fitplanner.ai.core.model.SessionStatus
import com.fitplanner.ai.core.model.TrainingWeek
import com.fitplanner.ai.core.model.WorkoutSession
import com.fitplanner.ai.core.model.fullLabel
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PlannerScreen(
    onStartWorkout: (Long) -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: PlannerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plan = state.plan
    var selectedWeekId by rememberSaveable(plan?.id) {
        mutableStateOf(plan?.activeWeek()?.id ?: 0L)
    }
    val selectedWeek = plan?.weeks?.firstOrNull { it.id == selectedWeekId } ?: plan?.activeWeek()
    if (state.showConfirmGenerate) {
        AlertDialog(
            onDismissRequest = viewModel::dismissGenerate,
            title = { Text("Generar una rutina nueva") },
            text = {
                Text(
                    "El plan activo se archivará y conservaremos su historial. La nueva rutina se creará con tu perfil, equipo y restricciones actuales."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::generate) { Text("Generar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissGenerate) { Text("Cancelar") }
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
                    text = "Planificador",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tu semana, con tiempo real para calentar, entrenar y recuperar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (state.loading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            return@LazyColumn
        }
        if (state.blockedTitle != null) {
            item {
                FpCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = state.blockedTitle!!,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = state.blockedDetail.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    FpPrimaryButton(
                        text = "Revisar mi perfil",
                        onClick = onOpenProfile
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
        if (state.plan == null) {
            if (state.blockedTitle == null) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "Diseñemos tu semana",
                        message = "El motor elegirá una división según tus días, nivel, objetivo y equipo. Después podrás revisar y ajustar las sesiones.",
                        action = {
                            FpPrimaryButton(
                                text = if (state.generating) "Generando..." else "Generar mi rutina",
                                enabled = !state.generating,
                                icon = Icons.Outlined.Refresh,
                                onClick = viewModel::requestGenerate
                            )
                        }
                    )
                }
            }
        } else {
            item {
                PlanSummary(plan = state.plan!!)
            }
            item {
                WeekSelector(
                    weeks = plan!!.weeks,
                    selectedWeekId = selectedWeek?.id,
                    onSelect = { selectedWeekId = it },
                    showOnlyActive = false
                )
            }
            val week = selectedWeek
            if (week != null) {
                items(DayOfWeek.values().toList()) { day ->
                    val date = week.startDate.plusDays(day.value.toLong() - 1)
                    val session = week.sessions.firstOrNull { it.date == date }
                    PlannerDayCard(
                        day = day,
                        session = session,
                        onStart = { session?.let { onStartWorkout(it.id) } }
                    )
                }
            }
            if (state.warnings.isNotEmpty()) {
                item {
                    FpCard(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Avisos del motor",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            state.warnings.forEach { warning ->
                                Text(
                                    text = "• $warning",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
            item {
                FpPrimaryButton(
                    text = if (state.generating) "Generando..." else "Generar nueva rutina",
                    enabled = !state.generating,
                    icon = Icons.Outlined.Refresh,
                    onClick = viewModel::requestGenerate
                )
            }
            item {
                Text(
                    text = "Cambiar de rutina no borra las semanas anteriores. Las sesiones completadas se conservan como historial.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlanSummary(plan: com.fitplanner.ai.core.model.TrainingPlan) {
    val week = plan.activeWeek()
    FpCard(containerColor = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = week?.focus.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(text = "Activo")
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            SummaryStat("${week?.sessions?.count { it.exercises.isNotEmpty() } ?: 0}", "Sesiones")
            SummaryStat("${week?.compliancePercent ?: 0}%", "Cumplido")
            SummaryStat("${week?.plannedSessions ?: 0}", "Planificadas")
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String) {
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
private fun WeekSelector(
    weeks: List<TrainingWeek>,
    selectedWeekId: Long?,
    onSelect: (Long) -> Unit,
    showOnlyActive: Boolean
) {
    if (weeks.size <= 1 && showOnlyActive) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(weeks) { week ->
            FilterChip(
                selected = week.id == selectedWeekId,
                onClick = { onSelect(week.id) },
                label = { Text("Semana ${week.weekNumber}") }
            )
        }
    }
}

@Composable
private fun PlannerDayCard(
    day: DayOfWeek,
    session: WorkoutSession?,
    onStart: () -> Unit
) {
    val completed = session?.status == SessionStatus.COMPLETED
    val isRest = session == null || session.exercises.isEmpty()
    FpCard(
        modifier = Modifier.clickable(enabled = session != null, onClick = onStart),
        borderColor = if (completed) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        when {
                            completed -> MaterialTheme.colorScheme.secondaryContainer
                            isRest -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        completed -> Icons.Outlined.Check
                        isRest -> Icons.Outlined.CalendarMonth
                        else -> Icons.Outlined.FitnessCenter
                    },
                    contentDescription = null,
                    tint = when {
                        completed -> MaterialTheme.colorScheme.onSecondaryContainer
                        isRest -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = day.fullLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (session?.date == java.time.LocalDate.now()) {
                        Spacer(Modifier.width(6.dp))
                        StatusPill(text = "Hoy")
                    }
                }
                Text(
                    text = when {
                        session == null -> "Descanso"
                        session.exercises.isEmpty() -> "Recuperación activa"
                        else -> session.name
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (session != null && session.exercises.isNotEmpty()) {
                    Text(
                        text = "${session.objective} · ${session.estimatedMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Muévete suave si te apetece. Sin obligación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (session != null && session.exercises.isNotEmpty()) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = "Empezar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
