package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Square
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BuiltInPalettes
import com.example.data.repository.ProjectRepository
import com.example.drawing.PixelCanvasState
import com.example.export.PngExporter
import com.example.model.DrawingTool
import com.example.model.PixelPalette
import com.example.model.PixelProject
import com.example.ui.components.ColorPickerDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.LayersDialog
import com.example.ui.components.PixelCanvasView
import com.example.ui.components.ResizeCanvasDialog
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

    val canvasState = remember(initialProject.id) {
        PixelCanvasState(initialProject)
    }

    var activePalette by remember {
        mutableStateOf(BuiltInPalettes.getById(initialProject.paletteId))
    }

    // Dialog flags
    var showColorDialog by remember { mutableStateOf(false) }
    var showLayersDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showResizeDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Export storage state
    var pendingExportScale by remember { mutableStateOf(8) }
    var pendingExportIncludeBg by remember { mutableStateOf(true) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        PngExporter.exportToStream(
                            project = canvasState.project,
                            scale = pendingExportScale,
                            includeBackground = pendingExportIncludeBg,
                            outputStream = out
                        )
                    }
                    Toast.makeText(context, "PNG saved successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Failed to export image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Debounced autosave
    LaunchedEffect(canvasState.hasUnsavedChanges) {
        if (canvasState.hasUnsavedChanges) {
            delay(1200) // Debounce 1.2s
            canvasState.syncBackToProject()
            repository.saveProject(canvasState.project)
            canvasState.hasUnsavedChanges = false
        }
    }

    // Save on back transition
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
                layerCount = canvasState.layers.size,
                onBack = { handleExit() },
                onTitleClick = { showRenameDialog = true },
                onUndo = { canvasState.undo() },
                onRedo = { canvasState.redo() },
                onResetView = { canvasState.resetView() },
                onToggleGrid = { canvasState.isGridVisible = !canvasState.isGridVisible },
                onOpenLayers = { showLayersDialog = true },
                onOpenExport = { showExportDialog = true },
                showMenu = showOverflowMenu,
                onMenuToggle = { showOverflowMenu = it },
                onResizeClick = { showResizeDialog = true },
                onClearCanvasClick = { showClearConfirm = true }
            )
        },
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
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(picked))
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
            }

            // Bottom Tools & Palette Panel
            EditorBottomPanel(
                activeTool = canvasState.activeTool,
                onToolSelect = { canvasState.activeTool = it },
                currentColor = canvasState.currentColor,
                onColorPickerOpen = { showColorDialog = true },
                activePalette = activePalette,
                onColorSwatchClick = { canvasState.currentColor = it },
                brushSize = canvasState.brushSize,
                onBrushSizeChange = { canvasState.brushSize = it }
            )
        }
    }

    // Dialogs
    if (showColorDialog) {
        ColorPickerDialog(
            initialColor = canvasState.currentColor,
            activePalette = activePalette,
            onPaletteSelect = {
                activePalette = it
                canvasState.project.paletteId = it.id
            },
            onColorSelected = { canvasState.currentColor = it },
            onDismiss = { showColorDialog = false }
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
            title = { Text("Clear Canvas") },
            text = { Text("Are you sure you want to clear active layer? You can still undo this action.") },
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
    layerCount: Int,
    onBack: () -> Unit,
    onTitleClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onResetView: () -> Unit,
    onToggleGrid: () -> Unit,
    onOpenLayers: () -> Unit,
    onOpenExport: () -> Unit,
    showMenu: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onResizeClick: () -> Unit,
    onClearCanvasClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ForgeSurface)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("editor_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onTitleClick() }
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                color = Color.White
            )
            Text(
                text = "$dimensions px",
                fontSize = 11.sp,
                color = ForgeCyan
            )
        }

        // Undo
        IconButton(
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier.testTag("editor_undo_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                tint = if (canUndo) Color.White else Color.DarkGray
            )
        }

        // Redo
        IconButton(
            onClick = onRedo,
            enabled = canRedo,
            modifier = Modifier.testTag("editor_redo_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
                tint = if (canRedo) Color.White else Color.DarkGray
            )
        }

        // Reset Pan/Zoom (fit-to-screen)
        IconButton(onClick = onResetView) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = "Reset Zoom",
                tint = Color.LightGray
            )
        }

        // Toggle Grid
        IconButton(onClick = onToggleGrid) {
            Icon(
                imageVector = Icons.Default.Grid4x4,
                contentDescription = "Toggle Grid",
                tint = if (isGridVisible) ForgeCyan else Color.DarkGray
            )
        }

        // Layers button with badge
        IconButton(
            onClick = onOpenLayers,
            modifier = Modifier.testTag("editor_layers_button")
        ) {
            BadgedBox(
                badge = {
                    Badge(containerColor = ForgePurple) {
                        Text("$layerCount")
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Layers",
                    tint = ForgeCyan
                )
            }
        }

        // Export PNG
        IconButton(
            onClick = onOpenExport,
            modifier = Modifier.testTag("editor_export_button")
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Export PNG",
                tint = ForgeEmerald
            )
        }

        // Overflow
        Box {
            IconButton(onClick = { onMenuToggle(true) }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { onMenuToggle(false) },
                modifier = Modifier.background(ForgeSurfaceElevated)
            ) {
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

@Composable
private fun EditorBottomPanel(
    activeTool: DrawingTool,
    onToolSelect: (DrawingTool) -> Unit,
    currentColor: Int,
    onColorPickerOpen: () -> Unit,
    activePalette: PixelPalette,
    onColorSwatchClick: (Int) -> Unit,
    brushSize: Int,
    onBrushSizeChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ForgeSurface)
            .border(width = 1.dp, color = Color(0xFF1E293B))
            .navigationBarsPadding()
            .padding(vertical = 8.dp)
    ) {
        // 1. Tool carousel with clear visual indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton(
                tool = DrawingTool.PENCIL,
                icon = Icons.Default.Brush,
                isActive = activeTool == DrawingTool.PENCIL,
                onClick = { onToolSelect(DrawingTool.PENCIL) }
            )
            ToolButton(
                tool = DrawingTool.ERASER,
                icon = Icons.Default.DeleteSweep,
                isActive = activeTool == DrawingTool.ERASER,
                onClick = { onToolSelect(DrawingTool.ERASER) }
            )
            ToolButton(
                tool = DrawingTool.BUCKET,
                icon = Icons.Default.FormatColorFill,
                isActive = activeTool == DrawingTool.BUCKET,
                onClick = { onToolSelect(DrawingTool.BUCKET) }
            )
            ToolButton(
                tool = DrawingTool.EYEDROPPER,
                icon = Icons.Default.Colorize,
                isActive = activeTool == DrawingTool.EYEDROPPER,
                onClick = { onToolSelect(DrawingTool.EYEDROPPER) }
            )
            ToolButton(
                tool = DrawingTool.LINE,
                icon = Icons.Default.Timeline,
                isActive = activeTool == DrawingTool.LINE,
                onClick = { onToolSelect(DrawingTool.LINE) }
            )
            ToolButton(
                tool = DrawingTool.RECTANGLE,
                icon = Icons.Outlined.Square,
                isActive = activeTool == DrawingTool.RECTANGLE,
                onClick = { onToolSelect(DrawingTool.RECTANGLE) }
            )
            ToolButton(
                tool = DrawingTool.FILLED_RECTANGLE,
                icon = Icons.Default.Square,
                isActive = activeTool == DrawingTool.FILLED_RECTANGLE,
                onClick = { onToolSelect(DrawingTool.FILLED_RECTANGLE) }
            )
            ToolButton(
                tool = DrawingTool.CIRCLE,
                icon = Icons.Default.RadioButtonUnchecked,
                isActive = activeTool == DrawingTool.CIRCLE,
                onClick = { onToolSelect(DrawingTool.CIRCLE) }
            )
            ToolButton(
                tool = DrawingTool.FILLED_CIRCLE,
                icon = Icons.Default.Circle,
                isActive = activeTool == DrawingTool.FILLED_CIRCLE,
                onClick = { onToolSelect(DrawingTool.FILLED_CIRCLE) }
            )
            ToolButton(
                tool = DrawingTool.MOVE,
                icon = Icons.Default.PanTool,
                isActive = activeTool == DrawingTool.MOVE,
                onClick = { onToolSelect(DrawingTool.MOVE) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Active Tool description banner & brush size selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tool: ${activeTool.title} • ${activeTool.description}",
                fontSize = 11.sp,
                color = ForgeCyan,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            // Brush size buttons (1px, 2px, 3px)
            if (activeTool == DrawingTool.PENCIL || activeTool == DrawingTool.ERASER) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (size in 1..3) {
                        val isSizeActive = brushSize == size
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSizeActive) ForgePurple else ForgeSurfaceElevated)
                                .clickable { onBrushSizeChange(size) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${size}p",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSizeActive) Color.White else Color.LightGray
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Color Bar: Active Color + Quick Palette Swatches
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Color Preview (tap opens dialog)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(currentColor))
                    .border(2.dp, ForgeCyan, RoundedCornerShape(10.dp))
                    .clickable { onColorPickerOpen() }
                    .testTag("active_color_swatch_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = "Color Studio",
                    tint = if ((currentColor ushr 24) == 0 || (Color(currentColor).red * 0.299 + Color(currentColor).green * 0.587 + Color(currentColor).blue * 0.114) > 0.6) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Quick swatches from active palette
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Transparent swatch
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { onColorSwatchClick(0) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("∅", color = Color.White, fontSize = 14.sp)
                }

                for (colorInt in activePalette.colors) {
                    val isCurrent = currentColor == colorInt
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(colorInt))
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) ForgeCyan else Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .clickable { onColorSwatchClick(colorInt) }
                    )
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
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) ForgePurple else ForgeSurfaceElevated)
            .border(
                width = 1.dp,
                color = if (isActive) ForgeCyan else Color(0xFF263352),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .testTag("tool_${tool.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tool.title,
            tint = if (isActive) Color.White else Color.LightGray,
            modifier = Modifier.size(22.dp)
        )
    }
}
