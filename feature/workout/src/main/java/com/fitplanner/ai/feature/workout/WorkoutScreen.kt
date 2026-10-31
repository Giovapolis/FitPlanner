package com.fitplanner.ai.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.fitplanner.ai.core.designsystem.components.ExerciseAnimation
import com.fitplanner.ai.core.designsystem.components.FpCard
import com.fitplanner.ai.core.designsystem.components.FpPrimaryButton
import com.fitplanner.ai.core.designsystem.components.FpSecondaryButton
import com.fitplanner.ai.core.designsystem.components.StatusPill
import com.fitplanner.ai.core.model.Exercise
import com.fitplanner.ai.core.model.SessionExercise
import com.fitplanner.ai.core.model.SessionStatus
import com.fitplanner.ai.core.model.WorkoutSession
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onBack: () -> Unit,
    onFinished: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showExitConfirm by remember { mutableStateOf(false) }
    var showFinishConfirm by remember { mutableStateOf(false) }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Salir de la sesión") },
            text = { Text("Las series que ya registraste se guardan. Podrás continuar después desde el plan.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    onBack()
                }) { Text("Salir") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("Seguir entrenando") }
            }
        )
    }
    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = { Text("Finalizar entrenamiento") },
            text = { Text("Se guardará la sesión con las series completadas. Si te queda algo pendiente, puedes dejarlo para otro momento.") },
            confirmButton = {
                TextButton(onClick = {
                    showFinishConfirm = false
                    viewModel.finish()
                }) { Text("Finalizar") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirm = false }) { Text("Cancelar") }
            }
        )
    }
    if (state.showAlternatives) {
        AlertDialog(
            onDismissRequest = viewModel::closeAlternatives,
            title = { Text("Sustituir ejercicio") },
            text = {
                Column(modifier = Modifier.heightIn(max = 470.dp)) {
                    if (state.alternatives.isEmpty()) {
                        Text("No encontramos una alternativa local compatible con tu equipo y restricciones.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            itemsIndexed(state.alternatives) { _, exercise ->
                                TextButton(
                                    onClick = { viewModel.substitute(exercise) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(exercise.nameEs)
                                        Text(
                                            text = "${exercise.primaryMuscle.label} · ${exercise.difficulty.label}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (state.aiMessage != null) {
                        Text(
                            text = state.aiMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    FpSecondaryButton(
                        text = if (state.aiLoading) "Consultando IA..." else "Pedir sugerencias a IA",
                        onClick = viewModel::requestAiAlternatives,
                        enabled = !state.aiLoading
                    )
                    Text(
                        text = "Solo se envía un resumen anonimizado. El motor local descarta lo que no cumpla equipo, restricciones o duración.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::closeAlternatives) { Text("Cerrar") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(state.session?.name ?: "Entrenamiento") },
                navigationIcon = {
                    IconButton(onClick = { showExitConfirm = true }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Salir")
                    }
                },
                actions = {
                    if (state.session?.status != SessionStatus.COMPLETED && !state.finished) {
                        TextButton(onClick = { showFinishConfirm = true }) {
                            Text("Finalizar")
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            state.session == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text(state.error ?: "Sesión no encontrada") }
            state.finished || state.session?.status == SessionStatus.COMPLETED -> CompletedContent(
                session = state.session!!,
                onFinished = onFinished,
                modifier = Modifier.padding(padding)
            )
            else -> ActiveWorkoutContent(
                state = state,
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ActiveWorkoutContent(
    state: WorkoutUiState,
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val session = state.session ?: return
    val current = state.currentExercise ?: return
    val exercise = state.exerciseDetails[current.exerciseId]
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(session.startedAt) {
        val started = session.startedAt ?: System.currentTimeMillis()
        while (true) {
            elapsed = ((System.currentTimeMillis() - started) / 1000).coerceAtLeast(0)
            delay(1000)
        }
    }
    val progress = (state.currentExerciseIndex + 1).toFloat() / session.exercises.size.coerceAtLeast(1)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ejercicio ${state.currentExerciseIndex + 1} de ${session.exercises.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatSeconds(elapsed.toInt()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(99.dp))
                )
            }
        }
        item {
            FpCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = current.exerciseName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = current.primaryMuscle.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(text = if (current.timeMinutes != null) "${current.timeMinutes} min" else "${current.sets} series")
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        WorkoutMeta(
                            if (current.timeMinutes != null) "${current.timeMinutes} min" else "${current.repsMin}-${current.repsMax}",
                            if (current.timeMinutes != null) "Duración" else "Repeticiones"
                        )
                        WorkoutMeta("${current.restSeconds}s", "Descanso")
                        WorkoutMeta("${current.completedSets}/${current.sets}", "Series")
                    }
                    if (current.techniqueCue.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = current.techniqueCue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (current.intensityNote.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = current.intensityNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        if (exercise != null) {
            item { ExerciseAnimation(exercise = exercise) }
        }
        item {
            SetLoggingCard(
                exercise = current,
                onLog = { reps, weight, rpe -> viewModel.logSet(reps, weight, rpe) }
            )
        }
        if (state.restRemaining > 0 || state.restRunning) {
            item {
                RestTimerCard(
                    remaining = state.restRemaining,
                    onAdd = { viewModel.addRest() },
                    onSkip = viewModel::skipRest
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = viewModel::previousExercise,
                    modifier = Modifier.weight(1f),
                    enabled = state.currentExerciseIndex > 0
                ) { Text("Anterior") }
                OutlinedButton(
                    onClick = viewModel::openAlternatives,
                    modifier = Modifier.weight(1f)
                ) { Text("Sustituir") }
                OutlinedButton(
                    onClick = viewModel::skipExercise,
                    modifier = Modifier.weight(1f)
                ) { Text("Saltar") }
            }
        }
        item {
            OutlinedButton(
                onClick = viewModel::nextExercise,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.currentExerciseIndex < session.exercises.lastIndex
            ) {
                Icon(Icons.Outlined.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Siguiente ejercicio")
            }
        }
        if (session.warmup.isNotEmpty()) {
            item {
                FpCard(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)) {
                    Column {
                        Text(
                            text = "Calentamiento",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        session.warmup.forEach { text ->
                            Text(
                                text = "• $text",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        if (session.cooldown.isNotEmpty()) {
            item {
                FpCard {
                    Column {
                        Text(
                            text = "Vuelta a la calma",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        session.cooldown.forEach { text ->
                            Text(
                                text = "• $text",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        item {
            FpPrimaryButton(
                text = "Finalizar entrenamiento",
                onClick = { viewModel.finish() }
            )
        }
    }
}

@Composable
private fun SetLoggingCard(
    exercise: SessionExercise,
    onLog: (Int, Double?, Int?) -> Unit
) {
    var repsText by remember(exercise.id) {
        mutableStateOf(
            if (exercise.timeMinutes != null) exercise.timeMinutes.toString()
            else ((exercise.repsMin + exercise.repsMax) / 2).toString()
        )
    }
    var weightText by remember(exercise.id) { mutableStateOf("") }
    var rpeText by remember(exercise.id) { mutableStateOf("") }
    FpCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Registrar series",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            exercise.logs.forEachIndexed { index, log ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Serie ${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (exercise.timeMinutes != null) "${log.reps} min"
                        else "${log.reps} reps${log.weightKg?.let { " · ${formatWeight(it)} kg" } ?: ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (exercise.logs.size < exercise.sets) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { text -> if (text.all { it.isDigit() }) repsText = text },
                        label = { Text(if (exercise.timeMinutes != null) "Min" else "Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (exercise.timeMinutes == null) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { text -> if (text.all { it.isDigit() || it == '.' }) weightText = text },
                            label = { Text("Kg") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = rpeText,
                        onValueChange = { text -> if (text.all { it.isDigit() }) rpeText = text },
                        label = { Text("RPE") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(78.dp)
                    )
                }
                FpPrimaryButton(
                    text = "Completar serie",
                    onClick = {
                        val reps = repsText.toIntOrNull() ?: return@FpPrimaryButton
                        onLog(reps, weightText.toDoubleOrNull(), rpeText.toIntOrNull())
                    }
                )
            } else {
                Text(
                    text = "Todas las series de este ejercicio están registradas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun RestTimerCard(remaining: Int, onAdd: () -> Unit, onSkip: () -> Unit) {
    FpCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatSeconds(remaining),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Descanso",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (remaining > 0) "Respira y prepara la siguiente serie." else "Descanso terminado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            TextButton(onClick = onAdd) { Text("+15s") }
            TextButton(onClick = onSkip) { Text("Saltar") }
        }
    }
}

@Composable
private fun CompletedContent(
    session: WorkoutSession,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val started = session.startedAt
    val completed = session.completedAt
    val duration = if (started != null && completed != null) {
        ((completed - started) / 1000).toInt()
    } else 0
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Entrenamiento completado",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            FpCard {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    WorkoutMeta(formatSeconds(duration), "Tiempo")
                    WorkoutMeta("${session.exercises.sumOf { it.logs.size }}", "Series")
                    WorkoutMeta("${session.completedExercises}/${session.exercises.size}", "Ejercicios")
                }
            }
        }
        item {
            FpCard(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) {
                Text(
                    text = "Tu registro se ha guardado en este dispositivo. La próxima semana puedes revisar cómo te sentó y ajustar con calma.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        item {
            FpPrimaryButton(text = "Volver al inicio", onClick = onFinished)
        }
    }
}

@Composable
private fun WorkoutMeta(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatSeconds(total: Int): String {
    val minutes = total / 60
    val seconds = total % 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        val rest = minutes % 60
        "%d:%02d:%02d".format(hours, rest, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun formatWeight(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
