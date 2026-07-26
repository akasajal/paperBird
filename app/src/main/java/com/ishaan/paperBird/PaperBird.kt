package com.ishaan.paperBird

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.*
import com.ishaan.paperBird.ui.navigation.*
import com.ishaan.paperBird.ui.screens.LetterViewModel
import com.ishaan.paperBird.ui.screens.calendar.CalendarScreen
import com.ishaan.paperBird.ui.screens.editor.EditorScreen
import com.ishaan.paperBird.ui.screens.favorites.FavoritesScreen
import com.ishaan.paperBird.ui.screens.home.HomeScreen
import com.ishaan.paperBird.ui.screens.library.LibraryScreen
import com.ishaan.paperBird.ui.screens.settings.SettingsScreen
import com.ishaan.paperBird.ui.screens.timeline.TimelineScreen
import kotlinx.coroutines.launch

@Composable
fun PaperBirdApp(viewModel: LetterViewModel) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    val isEditorRoute = currentRoute?.startsWith("editor") == true

    Scaffold(
        bottomBar = {
            if (!isEditorRoute) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = if (!isEditorRoute) Modifier.padding(innerPadding) else Modifier
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNewLetter = {
                        navController.navigate(Screen.Editor.createRoute())
                    },
                    onOpenLetter = { id ->
                        navController.navigate(Screen.Editor.createRoute(letterId = id))
                    }
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    viewModel = viewModel,
                    onOpenLetter = { id -> navController.navigate(Screen.Editor.createRoute(letterId = id)) }
                )
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    viewModel = viewModel,
                    onOpenLetter = { id -> navController.navigate(Screen.Editor.createRoute(letterId = id)) }
                )
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    viewModel = viewModel,
                    onOpenLetter = { id -> navController.navigate(Screen.Editor.createRoute(letterId = id)) }
                )
            }
            composable(Screen.Timeline.route) {
                TimelineScreen(
                    viewModel = viewModel,
                    onOpenLetter = { id -> navController.navigate(Screen.Editor.createRoute(letterId = id)) }
                )
            }
            composable(Screen.Settings.route) {
                val scope = rememberCoroutineScope()
                SettingsScreen(
                    viewModel = viewModel,
                    onThemeChange = { dark -> scope.launch { viewModel.settingsRepository.setDarkTheme(dark) } },
                    onAccentChange = { name -> scope.launch { viewModel.settingsRepository.setAccentColor(name) } }
                )
            }
            composable(
                route = "editor?letterId={letterId}",
                arguments = listOf(
                    navArgument("letterId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStackEntry ->
                val rawId = backStackEntry.arguments?.getLong("letterId") ?: -1L
                val letterId = if (rawId == -1L) null else rawId

                EditorScreen(
                    viewModel = viewModel,
                    letterId = letterId,
                    onBack = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
        }
    }
}