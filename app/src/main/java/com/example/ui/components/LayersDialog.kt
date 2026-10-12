package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drawing.PixelCanvasState
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgePurple
import com.example.ui.theme.ForgeSurface
import com.example.ui.theme.ForgeSurfaceElevated

@Composable
fun LayersDialog(
    state: PixelCanvasState,
    onDismiss: () -> Unit
) {
    var layerToRename by remember { mutableStateOf<Pair<Int, String>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Layers (${state.layers.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = { state.addLayer() },
                    modifier = Modifier.testTag("add_layer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Layer",
                        tint = ForgeCyan
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Reverse list for layer ordering: top layer appears on top of the list
                val reversedIndices = state.layers.indices.reversed().toList()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(reversedIndices) { _, actualIndex ->
                        val layer = state.layers[actualIndex]
                        val isActive = state.activeLayerIndex == actualIndex

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isActive) ForgeSurfaceElevated else Color(0xFF0F1526))
                                .border(
                                    width = if (isActive) 2.dp else 1.dp,
                                    color = if (isActive) ForgePurple else Color(0xFF263352),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { state.activeLayerIndex = actualIndex }
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Visibility toggle
                                IconButton(
                                    onClick = {
                                        state.setLayerVisibility(actualIndex, !layer.isVisible)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Visibility",
                                        tint = if (layer.isVisible) ForgeCyan else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Layer Name
                                Text(
                                    text = layer.name,
                                    color = if (isActive) Color.White else Color.LightGray,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                // Rename button
                                IconButton(
                                    onClick = { layerToRename = actualIndex to layer.name },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Duplicate
                                IconButton(
                                    onClick = {
                                        state.activeLayerIndex = actualIndex
                                        state.duplicateActiveLayer()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Duplicate",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete (if > 1 layer)
                                if (state.layers.size > 1) {
                                    IconButton(
                                        onClick = {
                                            state.activeLayerIndex = actualIndex
                                            state.deleteActiveLayer()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Layer",
                                            tint = Color(0xFFF87171),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Opacity slider for active layer
                            if (isActive) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Opacity: ${(layer.opacity * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        color = Color.LightGray,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Slider(
                                        value = layer.opacity,
                                        onValueChange = { state.setLayerOpacity(actualIndex, it) },
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = ForgeCyan,
                                            activeTrackColor = ForgeCyan
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reorder and merge controls for active layer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { state.moveActiveLayerUp() },
                            enabled = state.activeLayerIndex < state.layers.size - 1
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Move Layer Up",
                                tint = if (state.activeLayerIndex < state.layers.size - 1) ForgeCyan else Color.DarkGray
                            )
                        }
                        IconButton(
                            onClick = { state.moveActiveLayerDown() },
                            enabled = state.activeLayerIndex > 0
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Move Layer Down",
                                tint = if (state.activeLayerIndex > 0) ForgeCyan else Color.DarkGray
                            )
                        }
                    }

                    if (state.activeLayerIndex > 0) {
                        Button(
                            onClick = { state.mergeActiveLayerDown() },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeSurfaceElevated)
                        ) {
                            Icon(
                                Icons.Default.CallMerge,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = ForgeCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Merge Down", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ForgePurple)
            ) {
                Text("Done")
            }
        }
    )

    // Rename Dialog
    layerToRename?.let { (idx, currentName) ->
        var renameText by remember { mutableStateOf(currentName) }
        AlertDialog(
            onDismissRequest = { layerToRename = null },
            title = { Text("Rename Layer") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Layer Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    state.renameLayer(idx, renameText.trim().ifEmpty { "Layer" })
                    layerToRename = null
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { layerToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
