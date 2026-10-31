package com.fitplanner.ai.feature.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitplanner.ai.core.designsystem.components.EmptyState
import com.fitplanner.ai.core.designsystem.components.FpCard
import com.fitplanner.ai.core.designsystem.components.StatusPill
import com.fitplanner.ai.core.model.Difficulty
import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.Exercise
import com.fitplanner.ai.core.model.LocationType
import com.fitplanner.ai.core.model.MuscleGroup

@Composable
fun ExercisesScreen(
    onOpenExercise: (String) -> Unit,
    viewModel: ExercisesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Catálogo",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${state.all.size} ejercicios con técnica, variantes y precauciones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Buscar por nombre o músculo") },
                shape = MaterialTheme.shapes.small
            )
        }
        item {
            FilterRow {
                FilterChip(
                    selected = state.favoritesOnly,
                    onClick = { viewModel.setFavoritesOnly(!state.favoritesOnly) },
                    label = { Text("Favoritos") },
                    leadingIcon = {
                        Icon(
                            if (state.favoritesOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                Difficulty.values().forEach { difficulty ->
                    FilterChip(
                        selected = state.difficulty == difficulty,
                        onClick = { viewModel.setDifficulty(if (state.difficulty == difficulty) null else difficulty) },
                        label = { Text(difficulty.label) }
                    )
                }
            }
        }
        item {
            Text(
                text = "Grupo muscular",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            FilterRow {
                FilterChip(
                    selected = state.muscle == null,
                    onClick = { viewModel.setMuscle(null) },
                    label = { Text("Todos") }
                )
                MuscleGroup.values().forEach { muscle ->
                    FilterChip(
                        selected = state.muscle == muscle,
                        onClick = { viewModel.setMuscle(if (state.muscle == muscle) null else muscle) },
                        label = { Text(muscle.label) }
                    )
                }
            }
        }
        item {
            Text(
                text = "Lugar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            FilterRow {
                FilterChip(
                    selected = state.location == null,
                    onClick = { viewModel.setLocation(null) },
                    label = { Text("Todos") }
                )
                listOf(LocationType.HOME, LocationType.GYM).forEach { location ->
                    FilterChip(
                        selected = state.location == location,
                        onClick = { viewModel.setLocation(if (state.location == location) null else location) },
                        label = { Text(location.label) }
                    )
                }
            }
        }
        item {
            Text(
                text = "Equipamiento",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            FilterRow {
                FilterChip(
                    selected = state.equipment == null,
                    onClick = { viewModel.setEquipment(null) },
                    label = { Text("Todos") }
                )
                EquipmentType.values().forEach { equipment ->
                    FilterChip(
                        selected = state.equipment == equipment,
                        onClick = { viewModel.setEquipment(if (state.equipment == equipment) null else equipment) },
                        label = { Text(equipment.label) }
                    )
                }
            }
        }
        if (state.loading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
        } else if (state.filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.FitnessCenter,
                    title = "No encontramos ejercicios",
                    message = "Prueba con otro filtro o borra la búsqueda. El catálogo se guarda en tu dispositivo."
                )
            }
        } else {
            item {
                Text(
                    text = "${state.filtered.size} resultados",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(state.filtered, key = { it.id }) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    favorite = exercise.id in state.favorites,
                    onClick = { onOpenExercise(exercise.id) },
                    onToggleFavorite = { viewModel.toggleFavorite(exercise) }
                )
            }
        }
    }
}

@Composable
private fun FilterRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    favorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    FpCard(
        modifier = Modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.nameEs,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = listOf(exercise.primaryMuscle.label, exercise.difficulty.label).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = exercise.equipment.take(3).joinToString(" · ") { it.label },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (favorite) "Quitar de favoritos" else "Añadir a favoritos",
                    tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
