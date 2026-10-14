package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Square
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BuiltInPalettes
import com.example.data.repository.PaletteRepository
import com.example.data.repository.ProjectRepository
import com.example.drawing.PixelCanvasState
import com.example.drawing.PixelSelection
import com.example.export.PngExporter
import com.example.model.DitheringPattern
import com.example.model.DrawingTool
import com.example.model.PixelPalette
import com.example.model.PixelProject
import com.example.model.SymmetryMode
import com.example.ui.components.ExportDialog
import com.example.ui.components.LayersDialog
import com.example.ui.components.PaletteStudioDialog
import com.example.ui.components.PixelCanvasView
import com.example.ui.components.ReferenceImageDialog
import com.example.ui.components.ReplaceColorDialog
import com.example.ui.components.ResizeCanvasDialog
import com.example.ui.components.SpriteSheetDialog
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgeEmerald
import com.example.ui.theme.ForgePurple
import com.example.ui.theme.ForgeSurface
import com.example.ui.theme.ForgeSurfaceElevated
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    initialProject: PixelProject,
    repository: ProjectRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val paletteRepository = remember { PaletteRepository(context) }

    val canvasState = remember(initialProject.id) {
        PixelCanvasState(initialProject)
    }

    var activePalette by remember {
        mutableStateOf(paletteRepository.getPaletteById(canvasState.project.paletteId))
    }

    var showLayersDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showResizeDialog by remember { mutableStateOf(false) }
    var showReplaceColorDialog by remember { mutableStateOf(false) }
    var showReferenceDialog by remember { mutableStateOf(false) }
    var showSpriteSheetDialog by remember { mutableStateOf(false) }
    var showPaletteStudioDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSymmetryMenu by remember { mutableStateOf(false) }
    var showAnimationTimeline by remember { mutableStateOf(false) }

    // Export storage launcher
    var pendingExportScale by remember { mutableIntStateOf(8) }
    var pendingExportIncludeBg by remember { mutableStateOf(true) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val stream = context.contentResolver.openOutputStream(uri)
                    if (stream != null) {
                        stream.use { output ->
                            PngExporter.exportToStream(
                                project = canvasState.project,
                                scale = pendingExportScale,
                                includeBackground = pendingExportIncludeBg,
                                outputStream = output
                            )
                        }
                        Toast.makeText(context, "Saved to device successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Animation playback coroutine loop
    LaunchedEffect(canvasState.isPlayingAnimation, canvasState.animationFps) {
        if (canvasState.isPlayingAnimation && canvasState.frames.size > 1) {
            while (canvasState.isPlayingAnimation) {
                val delayTime = (1000L / canvasState.animationFps.coerceIn(1, 24))
                delay(delayTime)
                val nextIndex = (canvasState.currentFrameIndex + 1) % canvasState.frames.size
                canvasState.switchFrame(nextIndex)
            }
        }
    }

    // Autosave on exit
    fun handleExit() {
        coroutineScope.launch {
            canvasState.syncBackToProject()
            repository.saveProject(canvasState.project)
            onBackClick()
        }
    }

    BackHandler {
        handleExit()
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                title = canvasState.project.title,
                dimensions = "${canvasState.width}×${canvasState.height}",
                canUndo = canvasState.canUndo,
                canRedo = canvasState.canRedo,
                isGridVisible = canvasState.isGridVisible,
                symmetryMode = canvasState.symmetryMode,
                layerCount = canvasState.layers.size,
                isTilemapActive = canvasState.isTilemapMode,
                isAnimationTimelineActive = showAnimationTimeline,
                onBack = { handleExit() },
                onTitleClick = { showRenameDialog = true },
                onUndo = { canvasState.undo() },
                onRedo = { canvasState.redo() },
                onToggleSymmetryMenu = { showSymmetryMenu = it },
                showSymmetryMenu = showSymmetryMenu,
                onSelectSymmetry = {
                    canvasState.symmetryMode = it
                    showSymmetryMenu = false
                },
                onResetView = { canvasState.resetView() },
                onToggleGrid = { canvasState.isGridVisible = !canvasState.isGridVisible },
                onToggleTilemap = { canvasState.isTilemapMode = !canvasState.isTilemapMode },
                onToggleAnimation = { showAnimationTimeline = !showAnimationTimeline },
                onOpenLayers = { showLayersDialog = true },
                onOpenExport = { showExportDialog = true },
                showMenu = showOverflowMenu,
                onMenuToggle = { showOverflowMenu = it },
                onReplaceColorClick = { showReplaceColorDialog = true },
                onReferenceClick = { showReferenceDialog = true },
                onSpriteSheetClick = { showSpriteSheetDialog = true },
                onResizeClick = { showResizeDialog = true },
                onClearCanvasClick = { showClearConfirm = true }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive drawing canvas area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                PixelCanvasView(
                    state = canvasState,
                    modifier = Modifier.fillMaxSize()
                )

                // Loupe / picked color indicator badge
                canvasState.pickedColorPreview?.let { picked ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ForgeSurface)
                            .border(1.dp, ForgeCyan, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(picked))
                                    .border(1.dp, Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Sampled: #${String.format("%06X", picked and 0xFFFFFF)}",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Feature B: Floating Selection & Transform Toolbar
                if (canvasState.selection.hasActiveMask) {
                    FloatingSelectionToolbar(
                        isFloating = canvasState.selection.isFloating,
                        onLift = { canvasState.liftSelection() },
                        onFlipH = { canvasState.flipSelectionHorizontal() },
                        onFlipV = { canvasState.flipSelectionVertical() },
                        onRotate90 = { canvasState.rotateSelection90() },
                        onCut = { canvasState.cutSelection() },
                        onCopy = { canvasState.copySelection() },
                        onPaste = { canvasState.pasteSelection() },
                        onClear = { canvasState.clearSelection() },
                        onDeselect = { canvasState.deselect() },
                        hasClipboard = PixelSelection.clipboard != null,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    )
                }

                // Tilemap indicator watermark
                if (canvasState.isTilemapMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ForgeSurface.copy(alpha = 0.85f))
                            .border(1.dp, ForgeCyan, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("3×3 Seamless Tilemap Mode", fontSize = 10.sp, color = ForgeCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Feature H: Animation Timeline Bar
            AnimatedVisibility(
                visible = showAnimationTimeline,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AnimationTimelineBar(
                    state = canvasState,
                    onOpenGifExport = { showExportDialog = true }
                )
            }

            // Bottom Tools & Palette Panel
            EditorBottomPanel(
                activeTool = canvasState.activeTool,
                onToolSelect = { canvasState.activeTool = it },
                currentColor = canvasState.currentColor,
                onColorPickerOpen = { showPaletteStudioDialog = true },
                activePalette = activePalette,
                onColorSwatchClick = { canvasState.currentColor = it },
                brushSize = canvasState.brushSize,
                onBrushSizeChange = { canvasState.brushSize = it },
                ditheringPattern = canvasState.ditheringPattern,
                onDitheringPatternChange = { canvasState.ditheringPattern = it },
                shadingStrength = canvasState.shadingStrength,
                onShadingStrengthChange = { canvasState.shadingStrength = it },
                magicWandTolerance = canvasState.magicWandTolerance,
                onMagicWandToleranceChange = { canvasState.magicWandTolerance = it }
            )
        }
    }

    // Dialogs
    if (showPaletteStudioDialog) {
        PaletteStudioDialog(
            paletteRepository = paletteRepository,
            activePalette = activePalette,
            currentColor = canvasState.currentColor,
            onSelectPalette = {
                activePalette = it
                canvasState.project.paletteId = it.id
            },
            onSelectColor = { canvasState.currentColor = it },
            onDismiss = { showPaletteStudioDialog = false }
        )
    }

    if (showReplaceColorDialog) {
        ReplaceColorDialog(
            state = canvasState,
            activePalette = activePalette,
            onDismiss = { showReplaceColorDialog = false }
        )
    }

    if (showReferenceDialog) {
        ReferenceImageDialog(
            state = canvasState,
            onDismiss = { showReferenceDialog = false }
        )
    }

    if (showSpriteSheetDialog) {
        SpriteSheetDialog(
            project = canvasState.project,
            onDismiss = { showSpriteSheetDialog = false }
        )
    }

    if (showLayersDialog) {
        LayersDialog(
            state = canvasState,
            onDismiss = { showLayersDialog = false }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            project = canvasState.project,
            onSaveToStorage = { scale, includeBg ->
                pendingExportScale = scale
                pendingExportIncludeBg = includeBg
                val sanitized = canvasState.project.title.replace(Regex("[^a-zA-Z0-9_]"), "_")
                createDocumentLauncher.launch("${sanitized}_${canvasState.width}x${canvasState.height}.png")
            },
            onOpenSpriteSheet = { showSpriteSheetDialog = true },
            onDismiss = { showExportDialog = false }
        )
    }

    if (showResizeDialog) {
        ResizeCanvasDialog(
            state = canvasState,
            onDismiss = { showResizeDialog = false }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = ForgeSurface,
            title = { Text("Clear Active Layer") },
            text = { Text("Are you sure you want to clear the active layer? You can still undo this action.") },
            confirmButton = {
                Button(
                    onClick = {
                        canvasState.clearActiveLayer()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Clear Layer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showRenameDialog) {
        var renameTitle by remember { mutableStateOf(canvasState.project.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = ForgeSurface,
            title = { Text("Rename Artwork") },
            text = {
                OutlinedTextField(
                    value = renameTitle,
                    onValueChange = { renameTitle = it },
                    singleLine = true,
                    label = { Text("Title") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    canvasState.project.title = renameTitle.trim().ifEmpty { "Untitled" }
                    canvasState.hasUnsavedChanges = true
                    showRenameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EditorTopBar(
    title: String,
    dimensions: String,
    canUndo: Boolean,
    canRedo: Boolean,
    isGridVisible: Boolean,
    symmetryMode: SymmetryMode,
    layerCount: Int,
    isTilemapActive: Boolean,
    isAnimationTimelineActive: Boolean,
    onBack: () -> Unit,
    onTitleClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleSymmetryMenu: (Boolean) -> Unit,
    showSymmetryMenu: Boolean,
    onSelectSymmetry: (SymmetryMode) -> Unit,
    onResetView: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleTilemap: () -> Unit,
    onToggleAnimation: () -> Unit,
    onOpenLayers: () -> Unit,
    onOpenExport: () -> Unit,
    showMenu: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onReplaceColorClick: () -> Unit,
    onReferenceClick: () -> Unit,
    onSpriteSheetClick: () -> Unit,
    onResizeClick: () -> Unit,
    onClearCanvasClick: () -> Unit
) {
    Surface(
        color = ForgeSurface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Project title + dimensions
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTitleClick() }
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$dimensions px",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForgeCyan,
                        fontSize = 11.sp
                    )
                }

                // Action icons row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) Color.White else Color.DarkGray
                        )
                    }

                    IconButton(
                        onClick = onRedo,
                        enabled = canRedo,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) Color.White else Color.DarkGray
                        )
                    }

                    // Feature A: Symmetry dropdown button
                    Box {
                        IconButton(
                            onClick = { onToggleSymmetryMenu(!showSymmetryMenu) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Flip,
                                contentDescription = "Symmetry",
                                tint = if (symmetryMode != SymmetryMode.NONE) ForgeCyan else Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showSymmetryMenu,
                            onDismissRequest = { onToggleSymmetryMenu(false) },
                            modifier = Modifier.background(ForgeSurfaceElevated)
                        ) {
                            SymmetryMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            mode.title,
                                            color = if (symmetryMode == mode) ForgeCyan else Color.White,
                                            fontWeight = if (symmetryMode == mode) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = { onSelectSymmetry(mode) }
                                )
                            }
                        }
                    }

                    // Grid Toggle
                    IconButton(
                        onClick = onToggleGrid,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.Grid4x4,
                            contentDescription = "Toggle Grid",
                            tint = if (isGridVisible) ForgeCyan else Color.Gray
                        )
                    }

                    // Animation Timeline Toggle
                    IconButton(
                        onClick = onToggleAnimation,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.Animation,
                            contentDescription = "Animation Timeline",
                            tint = if (isAnimationTimelineActive) ForgePurple else Color.White
                        )
                    }

                    // Tilemap Mode Toggle
                    IconButton(
                        onClick = onToggleTilemap,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.GridOn,
                            contentDescription = "Tilemap Mode",
                            tint = if (isTilemapActive) ForgeCyan else Color.White
                        )
                    }

                    // Layers Button
                    IconButton(
                        onClick = onOpenLayers,
                        modifier = Modifier.size(38.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = ForgePurple,
                                    contentColor = Color.White
                                ) {
                                    Text(layerCount.toString(), fontSize = 9.sp)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = "Layers", tint = Color.White)
                        }
                    }

                    // Export Button
                    IconButton(
                        onClick = onOpenExport,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export", tint = ForgeCyan)
                    }

                    // Overflow Menu
                    Box {
                        IconButton(
                            onClick = { onMenuToggle(!showMenu) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { onMenuToggle(false) },
                            modifier = Modifier.background(ForgeSurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Replace Color Tool", color = Color.White) },
                                onClick = {
                                    onMenuToggle(false)
                                    onReplaceColorClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = ForgeCyan)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reference Image Overlay", color = Color.White) },
                                onClick = {
                                    onMenuToggle(false)
                                    onReferenceClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = ForgeCyan)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Sprite Sheet", color = Color.White) },
                                onClick = {
                                    onMenuToggle(false)
                                    onSpriteSheetClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.GridOn, contentDescription = null, tint = ForgeCyan)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reset Zoom / Fit Canvas", color = Color.White) },
                                onClick = {
                                    onMenuToggle(false)
                                    onResetView()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = ForgeCyan)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Resize / Crop Canvas", color = Color.White) },
                                onClick = {
                                    onMenuToggle(false)
                                    onResizeClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AspectRatio, contentDescription = null, tint = ForgeCyan)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Canvas Layer", color = Color(0xFFF87171)) },
                                onClick = {
                                    onMenuToggle(false)
                                    onClearCanvasClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFF87171))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Floating Selection & Transform Toolbar shown when region is selected.
 */
@Composable
private fun FloatingSelectionToolbar(
    isFloating: Boolean,
    onLift: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onRotate90: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    onDeselect: () -> Unit,
    hasClipboard: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ForgeSurface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(onClick = onFlipH, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Flip, contentDescription = "Flip H", tint = ForgeCyan, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onFlipV, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Flip V", tint = ForgeCyan, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onRotate90, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90", tint = ForgeCyan, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onCut, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.ContentCut, contentDescription = "Cut", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            if (hasClipboard) {
                IconButton(onClick = onPaste, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = ForgeEmerald, modifier = Modifier.size(18.dp))
                }
            }
            IconButton(onClick = onClear, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDeselect, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Clear, contentDescription = "Deselect", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Animation Timeline bar supporting frame addition, duplication, deletion, onion skinning, and playback.
 */
@Composable
private fun AnimationTimelineBar(
    state: PixelCanvasState,
    onOpenGifExport: () -> Unit
) {
    Surface(
        color = ForgeSurfaceElevated,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { state.isPlayingAnimation = !state.isPlayingAnimation },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (state.isPlayingAnimation) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlayingAnimation) "Pause" else "Play",
                            tint = ForgeCyan
                        )
                    }

                    Text("${state.animationFps} FPS", fontSize = 11.sp, color = Color.LightGray)

                    Spacer(modifier = Modifier.width(8.dp))

                    // Onion skinning toggle
                    FilterChip(
                        selected = state.isOnionSkinEnabled,
                        onClick = { state.isOnionSkinEnabled = !state.isOnionSkinEnabled },
                        label = { Text("Onion Skin", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ForgePurple),
                        modifier = Modifier.height(26.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { state.addFrame() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add Frame", tint = ForgeCyan)
                    }
                    IconButton(onClick = { state.duplicateCurrentFrame() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Dup Frame", tint = Color.LightGray)
                    }
                    if (state.frames.size > 1) {
                        IconButton(onClick = { state.deleteCurrentFrame() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Frame", tint = Color(0xFFF87171))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Frames strip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(state.frames) { idx, _ ->
                    val isActive = idx == state.currentFrameIndex
                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) ForgePurple else ForgeSurface)
                            .border(if (isActive) 1.5.dp else 0.5.dp, if (isActive) ForgeCyan else Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                            .clickable { state.switchFrame(idx) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "#${idx + 1}",
                            fontSize = 11.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) Color.White else Color.LightGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorBottomPanel(
    activeTool: DrawingTool,
    onToolSelect: (DrawingTool) -> Unit,
    currentColor: Int,
    onColorPickerOpen: () -> Unit,
    activePalette: PixelPalette,
    onColorSwatchClick: (Int) -> Unit,
    brushSize: Int,
    onBrushSizeChange: (Int) -> Unit,
    ditheringPattern: DitheringPattern,
    onDitheringPatternChange: (DitheringPattern) -> Unit,
    shadingStrength: Float,
    onShadingStrengthChange: (Float) -> Unit,
    magicWandTolerance: Int,
    onMagicWandToleranceChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ForgeSurface)
            .border(width = 1.dp, color = Color(0xFF1E293B))
            .navigationBarsPadding()
            .padding(vertical = 6.dp)
    ) {
        // 1. Tool carousel with comprehensive pixel art toolset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton(DrawingTool.PENCIL, Icons.Default.Brush, activeTool == DrawingTool.PENCIL) { onToolSelect(DrawingTool.PENCIL) }
            ToolButton(DrawingTool.ERASER, Icons.Default.DeleteSweep, activeTool == DrawingTool.ERASER) { onToolSelect(DrawingTool.ERASER) }
            ToolButton(DrawingTool.DITHER, Icons.Default.Grain, activeTool == DrawingTool.DITHER) { onToolSelect(DrawingTool.DITHER) }
            ToolButton(DrawingTool.SHADING_LIGHTEN, Icons.Default.WbSunny, activeTool == DrawingTool.SHADING_LIGHTEN) { onToolSelect(DrawingTool.SHADING_LIGHTEN) }
            ToolButton(DrawingTool.SHADING_DARKEN, Icons.Default.AutoAwesome, activeTool == DrawingTool.SHADING_DARKEN) { onToolSelect(DrawingTool.SHADING_DARKEN) }
            ToolButton(DrawingTool.BUCKET, Icons.Default.FormatColorFill, activeTool == DrawingTool.BUCKET) { onToolSelect(DrawingTool.BUCKET) }
            ToolButton(DrawingTool.MAGIC_WAND, Icons.Default.AutoAwesome, activeTool == DrawingTool.MAGIC_WAND) { onToolSelect(DrawingTool.MAGIC_WAND) }
            ToolButton(DrawingTool.RECT_SELECT, Icons.Default.SelectAll, activeTool == DrawingTool.RECT_SELECT) { onToolSelect(DrawingTool.RECT_SELECT) }
            ToolButton(DrawingTool.LASSO_SELECT, Icons.Default.Polyline, activeTool == DrawingTool.LASSO_SELECT) { onToolSelect(DrawingTool.LASSO_SELECT) }
            ToolButton(DrawingTool.EYEDROPPER, Icons.Default.Colorize, activeTool == DrawingTool.EYEDROPPER) { onToolSelect(DrawingTool.EYEDROPPER) }
            ToolButton(DrawingTool.LINE, Icons.Default.Timeline, activeTool == DrawingTool.LINE) { onToolSelect(DrawingTool.LINE) }
            ToolButton(DrawingTool.RECTANGLE, Icons.Outlined.Square, activeTool == DrawingTool.RECTANGLE) { onToolSelect(DrawingTool.RECTANGLE) }
            ToolButton(DrawingTool.FILLED_RECTANGLE, Icons.Default.Square, activeTool == DrawingTool.FILLED_RECTANGLE) { onToolSelect(DrawingTool.FILLED_RECTANGLE) }
            ToolButton(DrawingTool.CIRCLE, Icons.Default.RadioButtonUnchecked, activeTool == DrawingTool.CIRCLE) { onToolSelect(DrawingTool.CIRCLE) }
            ToolButton(DrawingTool.FILLED_CIRCLE, Icons.Default.Circle, activeTool == DrawingTool.FILLED_CIRCLE) { onToolSelect(DrawingTool.FILLED_CIRCLE) }
            ToolButton(DrawingTool.MOVE, Icons.Default.PanTool, activeTool == DrawingTool.MOVE) { onToolSelect(DrawingTool.MOVE) }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Active Tool description banner & sub-tool parameters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${activeTool.title}: ${activeTool.description}",
                fontSize = 11.sp,
                color = ForgeCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Dithering pattern dropdown
            if (activeTool == DrawingTool.DITHER) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DitheringPattern.entries.forEach { pat ->
                        val isPat = ditheringPattern == pat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isPat) ForgePurple else ForgeSurfaceElevated)
                                .clickable { onDitheringPatternChange(pat) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(pat.name.take(3), fontSize = 9.sp, color = if (isPat) Color.White else Color.Gray)
                        }
                    }
                }
            } else if (activeTool == DrawingTool.MAGIC_WAND) {
                Text("Tol: $magicWandTolerance%", fontSize = 10.sp, color = Color.White)
            } else if (activeTool == DrawingTool.PENCIL || activeTool == DrawingTool.ERASER ||
                activeTool == DrawingTool.SHADING_LIGHTEN || activeTool == DrawingTool.SHADING_DARKEN) {
                // Brush size buttons (1p, 2p, 3p, 4p)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (size in 1..4) {
                        val isSizeActive = brushSize == size
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSizeActive) ForgePurple else ForgeSurfaceElevated)
                                .clickable { onBrushSizeChange(size) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${size}p",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSizeActive) Color.White else Color.LightGray
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Color Bar: Active Color + Quick Palette Swatches
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Color Preview (tap opens Palette Studio)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(currentColor))
                    .border(2.dp, ForgeCyan, RoundedCornerShape(8.dp))
                    .clickable { onColorPickerOpen() }
                    .testTag("active_color_swatch_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = "Palette Studio",
                    tint = if ((currentColor ushr 24) == 0 || (Color(currentColor).red * 0.299 + Color(currentColor).green * 0.587 + Color(currentColor).blue * 0.114) > 0.6) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick swatches from active palette
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Transparent swatch
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF475569), RoundedCornerShape(6.dp))
                        .clickable { onColorSwatchClick(0) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("∅", fontSize = 14.sp, color = Color.White)
                }

                for (color in activePalette.colors) {
                    val isSelected = currentColor == color
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(color))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) ForgeCyan else Color(0x33000000),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onColorSwatchClick(color) }
                    )
                }

                // Palette Studio Quick Launcher Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ForgeSurfaceElevated)
                        .border(1.dp, ForgePurple, RoundedCornerShape(6.dp))
                        .clickable { onColorPickerOpen() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Studio", fontSize = 10.sp, color = ForgeCyan, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    tool: DrawingTool,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) ForgePurple else ForgeSurfaceElevated)
            .border(
                width = if (isActive) 1.5.dp else 0.5.dp,
                color = if (isActive) ForgeCyan else Color(0xFF263352),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .testTag("tool_${tool.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tool.title,
            tint = if (isActive) Color.White else Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
    }
}
