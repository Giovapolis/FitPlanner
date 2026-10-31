package com.fitplanner.ai.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitplanner.ai.core.designsystem.components.EmptyState
import com.fitplanner.ai.core.designsystem.components.FpCard
import com.fitplanner.ai.core.designsystem.components.FpPrimaryButton
import com.fitplanner.ai.core.designsystem.components.SectionHeader
import com.fitplanner.ai.core.designsystem.components.StatusPill
import com.fitplanner.ai.core.model.DashboardData
import com.fitplanner.ai.core.model.MuscleGroup
import com.fitplanner.ai.core.model.SessionStatus
import com.fitplanner.ai.core.model.TrainingWeek
import com.fitplanner.ai.core.model.WorkoutSession
import com.fitplanner.ai.core.model.shortLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    onOpenPlanner: () -> Unit,
    onStartWorkout: (Long) -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenProgress: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val dateLabel = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.forLanguageTag("es-ES")))
            .replaceFirstChar { it.uppercase() }
    }
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Buenos días"
        in 12..19 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "$greeting,",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = state.profile?.let { "vamos a por tu semana" } ?: "bienvenido a FitPlanner",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.activePlan == null) {
            item {
                EmptyState(
                    icon = Icons.Outlined.CalendarMonth,
                    title = "Tu primer plan empieza aquí",
                    message = "Crea una rutina adaptada a tus días, equipo y objetivos. Sin datos inventados: verás tu progreso real en cuanto completes tu primera sesión.",
                    action = { FpPrimaryButton(text = "Crear mi rutina", onClick = onOpenPlanner) }
                )
            }
        } else {
            item {
                WeekRail(
                    week = state.activeWeek,
                    today = today,
                    onOpenPlanner = onOpenPlanner
                )
            }
            item {
                TodayCard(
                    session = state.todaySession,
                    hasPlan = state.activePlan != null,
                    onStartWorkout = onStartWorkout,
                    onOpenPlanner = onOpenPlanner
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ComplianceCard(
                        modifier = Modifier.weight(1f),
                        percent = state.weeklyCompliance,
                        completed = state.completedThisWeek,
                        planned = state.plannedThisWeek
                    )
                    MuscleCard(
                        modifier = Modifier.weight(1f),
                        volume = state.muscleVolume
                    )
                }
            }
            if (state.plannedThisWeek > 0 && state.completedThisWeek >= state.plannedThisWeek) {
                item {
                    FpCard(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)) {
                        Column {
                            Text(
                                text = "Semana completada",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Cuéntanos cómo fue para ajustar la siguiente semana con prudencia.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(10.dp))
                            FpPrimaryButton(text = "Revisar semana", onClick = onOpenProgress)
                        }
                    }
                }
            }
            if (state.nextSession != null) {
                item {
                    NextSessionCard(
                        session = state.nextSession!!,
                        onClick = { onStartWorkout(state.nextSession!!.id) }
                    )
                }
            }
            if (state.lastCompletedSession != null) {
                item {
                    LastSessionCard(
                        session = state.lastCompletedSession!!,
                        onOpenProgress = onOpenProgress
                    )
                }
            }
            item {
                QuickActions(
                    onOpenCatalog = onOpenCatalog,
                    onOpenProgress = onOpenProgress
                )
            }
            item {
                FpCard(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.SelfImprovement,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "La constancia importa más que la intensidad. Si hoy no puedes con todo, camina, moviliza o descansa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekRail(
    week: TrainingWeek?,
    today: LocalDate,
    onOpenPlanner: () -> Unit
) {
    FpCard(
        modifier = Modifier.clickable { onOpenPlanner() },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Semana ${week?.weekNumber ?: 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = week?.focus ?: "Sin plan activo",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                StatusPill(text = "${week?.compliancePercent ?: 0} % cumplido")
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val sessions = week?.sessions.orEmpty()
                DayOfWeek.values().forEach { day ->
                    val date = (week?.startDate ?: today).plusDays(day.value.toLong() - 1)
                    val session = sessions.firstOrNull { it.date == date }
                    DayCell(
                        day = day,
                        isToday = date == today,
                        session = session
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Toca para ver o ajustar la semana",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp).clip(CircleShape)
                )
            }
        }
    }
}

@Composable
private fun DayCell(day: DayOfWeek, isToday: Boolean, session: WorkoutSession?) {
    val completed = session?.status == SessionStatus.COMPLETED
    val isRecovery = session != null && session.exercises.isEmpty()
    val hasTraining = session != null && session.exercises.isNotEmpty()
    val bg = when {
        completed -> MaterialTheme.colorScheme.secondary
        hasTraining -> MaterialTheme.colorScheme.primaryContainer
        isRecovery -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val fg = when {
        completed -> MaterialTheme.colorScheme.onSecondary
        hasTraining -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(38.dp)
    ) {
        Text(
            text = day.shortLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            when {
                completed -> Icon(Icons.Outlined.Check, null, tint = fg, modifier = Modifier.size(16.dp))
                hasTraining -> Icon(Icons.Outlined.FitnessCenter, null, tint = fg, modifier = Modifier.size(15.dp))
                isRecovery -> Icon(Icons.Outlined.Bedtime, null, tint = fg, modifier = Modifier.size(15.dp))
                else -> Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(fg.copy(alpha = 0.45f))
                )
            }
        }
    }
}

@Composable
private fun TodayCard(
    session: WorkoutSession?,
    hasPlan: Boolean,
    onStartWorkout: (Long) -> Unit,
    onOpenPlanner: () -> Unit
) {
    when {
        session == null -> {
            FpCard(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Bedtime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasPlan) "Hoy toca recuperar" else "Hoy no hay sesión",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (hasPlan) "Camina, moviliza o descansa. El descanso también entrena."
                            else "Genera tu plan para ver tu sesión de hoy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                if (!hasPlan) {
                    Spacer(Modifier.height(12.dp))
                    FpPrimaryButton(text = "Crear rutina", onClick = onOpenPlanner)
                }
            }
        }
        session.exercises.isEmpty() -> {
            FpCard(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.SelfImprovement,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Recuperación activa",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "20 min de movilidad suave o caminata. Sin obligación de rendir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        else -> {
            FpCard(
                containerColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        StatusPill(
                            text = if (session.status == SessionStatus.COMPLETED) "Completado" else "Sesión de hoy",
                            color = if (session.status == SessionStatus.COMPLETED) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = session.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = session.objective,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MetaItem("${session.estimatedMinutes} min", "Duración")
                    MetaItem("${session.exercises.size} ejercicios", "Sesión")
                    MetaItem(session.location.label, "Lugar")
                }
                Spacer(Modifier.height(16.dp))
                FpPrimaryButton(
                    text = if (session.status == SessionStatus.COMPLETED) "Ver sesión" else "Empezar entrenamiento",
                    icon = Icons.Outlined.PlayArrow,
                    onClick = { onStartWorkout(session.id) }
                )
            }
        }
    }
}

@Composable
private fun MetaItem(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
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
private fun ComplianceCard(modifier: Modifier, percent: Int, completed: Int, planned: Int) {
    FpCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MetricRing(percent = percent)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "$completed de $planned sesiones",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MetricRing(percent: Int) {
    val animated by animateFloatAsState(targetValue = percent / 100f, label = "compliance")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val accent = if (percent >= 70) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(74.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "semana",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MuscleCard(modifier: Modifier, volume: Map<MuscleGroup, Int>) {
    FpCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Whatshot,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Series",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(8.dp))
            if (volume.isEmpty()) {
                Text(
                    text = "Aún no hay series completadas esta semana.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                volume.entries.sortedByDescending { it.value }.take(3).forEach { (muscle, sets) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = muscle.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$sets",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NextSessionCard(session: WorkoutSession, onClick: () -> Unit) {
    FpCard(modifier = Modifier.clickable { onClick() }, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Próximo entrenamiento",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${session.name} · ${session.date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es-ES")))}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LastSessionCard(session: WorkoutSession, onOpenProgress: () -> Unit) {
    FpCard(modifier = Modifier.clickable { onOpenProgress() }, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Último entrenamiento",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = session.date.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-ES"))),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActions(onOpenCatalog: () -> Unit, onOpenProgress: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FpCard(
            modifier = Modifier.weight(1f).clickable { onOpenCatalog() },
            contentPadding = PaddingValues(14.dp)
        ) {
            Column {
                Icon(
                    Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Catálogo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Técnica y variantes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        FpCard(
            modifier = Modifier.weight(1f).clickable { onOpenProgress() },
            contentPadding = PaddingValues(14.dp)
        ) {
            Column {
                Icon(
                    Icons.Outlined.Insights,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Progreso",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Historial real",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
