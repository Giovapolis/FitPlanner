package com.fitplanner.ai

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitplanner.ai.feature.dashboard.DashboardScreen
import com.fitplanner.ai.feature.exercises.ExerciseDetailScreen
import com.fitplanner.ai.feature.exercises.ExercisesScreen
import com.fitplanner.ai.feature.planner.PlannerScreen
import com.fitplanner.ai.feature.profile.ProfileScreen
import com.fitplanner.ai.feature.progress.ProgressScreen
import com.fitplanner.ai.feature.workout.WorkoutScreen

private data class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val topDestinations = listOf(
    TopDestination("dashboard", "Inicio", Icons.Outlined.Home),
    TopDestination("planner", "Plan", Icons.Outlined.CalendarMonth),
    TopDestination("exercises", "Ejercicios", Icons.Outlined.FitnessCenter),
    TopDestination("progress", "Progreso", Icons.Outlined.Insights),
    TopDestination("profile", "Perfil", Icons.Outlined.Person)
)

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = topDestinations.any { it.route == currentRoute }

    fun navigateTop(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navigateTop(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    onOpenPlanner = { navigateTop("planner") },
                    onStartWorkout = { sessionId -> navController.navigate("workout/$sessionId") },
                    onOpenCatalog = { navigateTop("exercises") },
                    onOpenProgress = { navigateTop("progress") }
                )
            }
            composable("planner") {
                PlannerScreen(
                    onStartWorkout = { sessionId -> navController.navigate("workout/$sessionId") },
                    onOpenProfile = { navigateTop("profile") }
                )
            }
            composable("exercises") {
                ExercisesScreen(
                    onOpenExercise = { exerciseId -> navController.navigate("exercise/$exerciseId") }
                )
            }
            composable("progress") {
                ProgressScreen(
                    onOpenPlanner = { navigateTop("planner") }
                )
            }
            composable("profile") {
                ProfileScreen(
                    onOpenPlanner = { navigateTop("planner") }
                )
            }
            composable(
                route = "workout/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) {
                WorkoutScreen(
                    onBack = { navController.popBackStack() },
                    onFinished = {
                        navController.navigate("dashboard") {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                route = "exercise/{exerciseId}",
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
            ) {
                ExerciseDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
