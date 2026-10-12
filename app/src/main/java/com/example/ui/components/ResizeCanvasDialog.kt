package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drawing.PixelCanvasState
import com.example.model.PixelLayer
import com.example.ui.theme.ForgePurple
import com.example.ui.theme.ForgeSurface

@Composable
fun ResizeCanvasDialog(
    state: PixelCanvasState,
    onDismiss: () -> Unit
) {
    var newWText by remember { mutableStateOf(state.width.toString()) }
    var newHText by remember { mutableStateOf(state.height.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeSurface,
        title = {
            Text(
                "Resize / Crop Canvas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Current: ${state.width}×${state.height} px. Pixels outside new dimensions will be clipped.",
                    fontSize = 13.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newWText,
                        onValueChange = { newWText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("New Width") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newHText,
                        onValueChange = { newHText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("New Height") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetW = (newWText.toIntOrNull() ?: state.width).coerceIn(8, 128)
                    val targetH = (newHText.toIntOrNull() ?: state.height).coerceIn(8, 128)

                    if (targetW != state.width || targetH != state.height) {
                        state.history.pushState(state.layers, state.activeLayerIndex, "Resize Canvas")
                        val oldW = state.width
                        val oldH = state.height

                        // Resample all layers
                        for (i in state.layers.indices) {
                            val oldLayer = state.layers[i]
                            val newPixels = IntArray(targetW * targetH)
                            for (y in 0 until minOf(oldH, targetH)) {
                                for (x in 0 until minOf(oldW, targetW)) {
                                    newPixels[y * targetW + x] = oldLayer.getPixel(x, y, oldW)
                                }
                            }
                            state.layers[i] = PixelLayer(
                                id = oldLayer.id,
                                name = oldLayer.name,
                                isVisible = oldLayer.isVisible,
                                opacity = oldLayer.opacity,
                                pixels = newPixels
                            )
                        }
                        state.width = targetW
                        state.height = targetH
                        state.project = state.project.copy(
                            width = targetW,
                            height = targetH
                        )
                        state.hasUnsavedChanges = true
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForgePurple)
            ) {
                Text("Apply Resize")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
