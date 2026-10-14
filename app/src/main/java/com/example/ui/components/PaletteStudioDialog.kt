package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.PaletteRepository
import com.example.export.ColorQuantizer
import com.example.model.PixelPalette
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgePurple
import com.example.ui.theme.ForgeSurface
import com.example.ui.theme.ForgeSurfaceElevated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PaletteStudioDialog(
    paletteRepository: PaletteRepository,
    activePalette: PixelPalette,
    currentColor: Int,
    onSelectPalette: (PixelPalette) -> Unit,
    onSelectColor: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allPalettes by paletteRepository.palettesFlow.collectAsState()

    var selectedPaletteId by remember { mutableStateOf(activePalette.id) }
    val currentSelectedPalette = remember(allPalettes, selectedPaletteId) {
        allPalettes.find { it.id == selectedPaletteId } ?: activePalette
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var extractionTargetCount by remember { mutableIntStateOf(16) }
    var isExtracting by remember { mutableStateOf(false) }

    // Photo picker for image color extraction
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isExtracting = true
            scope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val options = BitmapFactory.Options().apply {
                                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                            }
                            BitmapFactory.decodeStream(stream, null, options)
                        }
                    }

                    if (bitmap != null) {
                        val extractedColors = withContext(Dispatchers.Default) {
                            ColorQuantizer.extractPalette(bitmap, extractionTargetCount)
                        }
                        val created = paletteRepository.saveCustomPalette(
                            "Extracted (${extractionTargetCount}c)",
                            extractedColors
                        )
                        selectedPaletteId = created.id
                        onSelectPalette(created)
                        Toast.makeText(context, "Extracted ${extractedColors.size} colors into new palette!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Could not decode image", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Extraction error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isExtracting = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(16.dp)),
            color = ForgeSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = ForgeCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Palette Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Extract from Image bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ForgeSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Extract from Photo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(8, 16, 32).forEach { count ->
                                FilterChip(
                                    selected = extractionTargetCount == count,
                                    onClick = { extractionTargetCount = count },
                                    label = { Text("${count}c", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ForgePurple
                                    ),
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isExtracting,
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isExtracting) "Extracting..." else "Pick Image", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Palettes Horizontal Selector
                Text("Select Palette:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allPalettes) { pal ->
                        val isSelected = pal.id == selectedPaletteId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ForgePurple.copy(alpha = 0.4f) else ForgeSurfaceElevated)
                                .border(1.dp, if (isSelected) ForgeCyan else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedPaletteId = pal.id
                                    onSelectPalette(pal)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    pal.name,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (pal.isCustom) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("(Custom)", fontSize = 9.sp, color = ForgeCyan)
                                }
                            }
                        }
                    }

                    // + New Custom Palette button
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ForgeSurfaceElevated)
                                .border(1.dp, ForgePurple, RoundedCornerShape(8.dp))
                                .clickable { showCreateDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = ForgeCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New", fontSize = 12.sp, color = ForgeCyan)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Palette Swatches and Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${currentSelectedPalette.name} (${currentSelectedPalette.colors.size} colors)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Add current color to palette
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val updated = paletteRepository.addColorToPalette(currentSelectedPalette.id, currentColor)
                                    if (updated != null) {
                                        selectedPaletteId = updated.id
                                        onSelectPalette(updated)
                                        Toast.makeText(context, "Added current color to palette!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Color", tint = ForgeCyan)
                        }

                        if (currentSelectedPalette.isCustom) {
                            IconButton(
                                onClick = { showRenameDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color.LightGray)
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        paletteRepository.deleteCustomPalette(currentSelectedPalette.id)
                                        selectedPaletteId = allPalettes.first().id
                                        onSelectPalette(allPalettes.first())
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF87171))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Color Swatches Grid with Hex values
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 64.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(currentSelectedPalette.colors) { colorVal ->
                        val isCurr = currentColor == colorVal
                        val hex = String.format("#%06X", colorVal and 0xFFFFFF)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ForgeSurfaceElevated)
                                .border(if (isCurr) 2.dp else 1.dp, if (isCurr) ForgeCyan else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                .clickable { onSelectColor(colorVal) }
                                .padding(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(colorVal))
                                    .border(0.5.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCurr) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if ((colorVal and 0xFFFFFF) > 0x888888) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                hex,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurr) ForgeCyan else Color.LightGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onSelectPalette(currentSelectedPalette)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ForgePurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Apply Palette in Editor")
                }
            }
        }
    }

    // Create New Custom Palette Dialog
    if (showCreateDialog) {
        var palName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = ForgeSurface,
            title = { Text("Create Custom Palette") },
            text = {
                OutlinedTextField(
                    value = palName,
                    onValueChange = { palName = it },
                    singleLine = true,
                    label = { Text("Palette Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val created = paletteRepository.saveCustomPalette(palName, listOf(currentColor))
                        selectedPaletteId = created.id
                        onSelectPalette(created)
                        showCreateDialog = false
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Rename Palette Dialog
    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(currentSelectedPalette.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = ForgeSurface,
            title = { Text("Rename Palette") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    label = { Text("New Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        paletteRepository.updateCustomPalette(currentSelectedPalette.copy(name = newTitle))
                        showRenameDialog = false
                    }
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
