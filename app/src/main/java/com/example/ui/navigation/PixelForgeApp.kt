package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.ForgeBg
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgePurple
import com.example.ui.theme.ForgeSurface
import com.example.ui.theme.ForgeSurfaceElevated
import com.example.ui.theme.PixelForgeTheme
import kotlinx.coroutines.delay
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
    var isSplashVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(850)
        isSplashVisible = false
    }

    PixelForgeTheme(darkTheme = isDarkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    // Hide bottom bar when editing to maximize drawing screen area
                    if (activeProject == null && !isSplashVisible) {
                        CompactPixelBottomBar(
                            currentDestination = currentDestination,
                            onDestinationSelected = { currentDestination = it }
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
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
                                },
                                modifier = screenModifier
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
                                },
                                modifier = screenModifier
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
                                onCreateNewClick = { showCreateDialog = true },
                                modifier = screenModifier
                            )
                        }

                        AppDestination.SETTINGS -> {
                            SettingsScreen(
                                preferences = preferences,
                                repository = repository,
                                modifier = screenModifier
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

            // Real Animated Splash Screen
            AnimatedVisibility(
                visible = isSplashVisible,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
            ) {
                PixelForgeSplashScreen()
            }
        }
    }
}

/**
 * Compact, sleek, responsive Material 3 bottom navigation bar.
 * Height is ~58dp (+ system navigation bar padding) with compact icons,
 * readable typography, and no oversized pills or label clipping.
 */
@Composable
private fun CompactPixelBottomBar(
    currentDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit
) {
    Surface(
        color = ForgeSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFF1E293B)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(60.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppDestination.entries.forEach { destination ->
                val isSelected = currentDestination == destination

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDestinationSelected(destination) }
                        .padding(vertical = 4.dp)
                        .testTag("nav_item_${destination.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 26.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) ForgePurple.copy(alpha = 0.35f) else Color.Transparent
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) ForgePurple else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                tint = if (isSelected) ForgeCyan else Color(0xFFA5B4CF),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = destination.label,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ForgeCyan else Color(0xFFA5B4CF),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Branded native splash screen with centered emblem and wordmark.
 */
@Composable
private fun PixelForgeSplashScreen() {
    var animateIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateIn = true
    }

    val scale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.8f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "splashScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(400),
        label = "splashAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        ) {
            // Emblem
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF22D3EE)))
                    )
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = "PixelForge Symbol",
                        tint = ForgeCyan,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PIXELFORGE",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = ForgeCyan
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Retro Pixel Art Studio",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA5B4CF),
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
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
