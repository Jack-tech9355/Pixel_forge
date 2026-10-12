package com.example.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocalPreferences
import com.example.data.repository.ProjectRepository
import com.example.model.PixelLayer
import com.example.model.PixelProject
import com.example.model.TemplateItem
import com.example.ui.components.CreateProjectDialog
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgePurple
import com.example.ui.theme.ForgeSurface
import com.example.ui.theme.PixelForgeTheme
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    TEMPLATES("Templates", Icons.Default.AutoAwesome),
    GALLERY("Gallery", Icons.Default.Collections),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun PixelForgeApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val repository = remember { ProjectRepository(context) }
    val preferences = remember { LocalPreferences(context) }

    val themeMode by preferences.themeMode.collectAsState(initial = "DARK")
    val defaultCanvasSize by preferences.defaultCanvasSize.collectAsState(initial = 32)

    val isDarkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
    var activeProject by remember { mutableStateOf<PixelProject?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    PixelForgeTheme(darkTheme = isDarkTheme) {
        Scaffold(
            bottomBar = {
                // Hide bottom bar when editing to maximize drawing screen area
                if (activeProject == null) {
                    NavigationBar(
                        containerColor = ForgeSurface,
                        tonalElevation = 8.dp
                    ) {
                        AppDestination.entries.forEach { destination ->
                            val isSelected = currentDestination == destination
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.label,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = ForgeCyan,
                                    indicatorColor = ForgePurple,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                ),
                                modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (activeProject != null) {
                EditorScreen(
                    initialProject = activeProject!!,
                    repository = repository,
                    onBackClick = {
                        activeProject = null
                    }
                )
            } else {
                val screenModifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)

                when (currentDestination) {
                    AppDestination.HOME -> {
                        HomeScreen(
                            repository = repository,
                            onCreateNewClick = { showCreateDialog = true },
                            onOpenProjectClick = { projId ->
                                coroutineScope.launch {
                                    val proj = repository.getProject(projId)
                                    if (proj != null) {
                                        activeProject = proj
                                    }
                                }
                            },
                            onOpenTemplateClick = { template ->
                                coroutineScope.launch {
                                    val newProj = createProjectFromTemplate(template)
                                    repository.saveProject(newProj)
                                    activeProject = newProj
                                }
                            },
                            onViewGalleryClick = {
                                currentDestination = AppDestination.GALLERY
                            }
                        )
                    }

                    AppDestination.TEMPLATES -> {
                        TemplatesScreen(
                            onSelectTemplate = { template ->
                                coroutineScope.launch {
                                    val newProj = createProjectFromTemplate(template)
                                    repository.saveProject(newProj)
                                    activeProject = newProj
                                }
                            }
                        )
                    }

                    AppDestination.GALLERY -> {
                        GalleryScreen(
                            repository = repository,
                            onOpenProject = { projId ->
                                coroutineScope.launch {
                                    val proj = repository.getProject(projId)
                                    if (proj != null) {
                                        activeProject = proj
                                    }
                                }
                            },
                            onCreateNewClick = { showCreateDialog = true }
                        )
                    }

                    AppDestination.SETTINGS -> {
                        SettingsScreen(
                            preferences = preferences,
                            repository = repository
                        )
                    }
                }
            }

            if (showCreateDialog) {
                CreateProjectDialog(
                    initialSize = defaultCanvasSize,
                    onCreate = { title, w, h, bgColor ->
                        val project = PixelProject.createEmpty(
                            title = title,
                            width = w,
                            height = h,
                            backgroundColor = bgColor
                        )
                        coroutineScope.launch {
                            repository.saveProject(project)
                            activeProject = project
                            showCreateDialog = false
                        }
                    },
                    onDismiss = { showCreateDialog = false }
                )
            }
        }
    }
}

private fun createProjectFromTemplate(template: TemplateItem): PixelProject {
    val pixels = template.pixelGenerator()
    return PixelProject(
        id = UUID.randomUUID().toString(),
        title = template.title,
        width = template.width,
        height = template.height,
        layers = mutableListOf(
            PixelLayer(
                name = "Base Layer",
                pixels = pixels
            )
        )
    )
}
