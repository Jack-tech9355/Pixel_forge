package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drawing.BresenhamAlgorithms
import com.example.drawing.PixelCanvasState
import com.example.model.PixelPalette
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgePurple
import com.example.ui.theme.ForgeSurface
import com.example.ui.theme.ForgeSurfaceElevated

@Composable
fun ReplaceColorDialog(
    state: PixelCanvasState,
    activePalette: PixelPalette,
    onDismiss: () -> Unit
) {
    var targetColor by remember { mutableIntStateOf(state.currentColor) }
    var replacementColor by remember {
        mutableIntStateOf(
            activePalette.colors.firstOrNull { it != state.currentColor } ?: 0xFFFFFFFF.toInt()
        )
    }
    var tolerance by remember { mutableFloatStateOf(0f) }
    var allLayers by remember { mutableStateOf(false) }

    // Count candidate matching pixels
    val matchingPixelsCount = remember(targetColor, tolerance, allLayers, state.renderVersion) {
        var count = 0
        val targetLayerList = if (allLayers) state.layers else listOfNotNull(state.activeLayer)
        for (l in targetLayerList) {
            for (p in l.pixels) {
                val diff = if (tolerance <= 0f) {
                    if (p == targetColor) 0.0 else 100.0
                } else {
                    BresenhamAlgorithms.colorDistance(p, targetColor)
                }
                if (diff <= tolerance) count++
            }
        }
        count
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = ForgeCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Replace Color",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Swap a specific color across your drawing with another color.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Before and After swatch visualizer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ForgeSurfaceElevated)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Target Color", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(targetColor))
                                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        )
                        Text("#${String.format("%06X", targetColor and 0xFFFFFF)}", fontSize = 10.sp, color = Color.LightGray)
                    }

                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = ForgeCyan)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("New Color", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(replacementColor))
                                .border(1.dp, ForgeCyan, RoundedCornerShape(8.dp))
                        )
                        Text("#${String.format("%06X", replacementColor and 0xFFFFFF)}", fontSize = 10.sp, color = ForgeCyan)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pick replacement color from active palette
                Text("Select Replacement Color:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(activePalette.colors) { c ->
                        val isSel = replacementColor == c
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(c))
                                .border(if (isSel) 2.dp else 1.dp, if (isSel) ForgeCyan else Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                .clickable { replacementColor = c }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tolerance slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tolerance: ${tolerance.toInt()}%", fontSize = 12.sp, color = Color.LightGray)
                    Text("Matches: $matchingPixelsCount px", fontSize = 12.sp, color = ForgeCyan, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = tolerance,
                    onValueChange = { tolerance = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = ForgeCyan,
                        activeTrackColor = ForgeCyan
                    )
                )

                // Scope: Layer vs Project
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !allLayers,
                        onClick = { allLayers = false },
                        label = { Text("Active Layer (${state.activeLayer?.name ?: "Layer 1"})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForgePurple
                        )
                    )
                    FilterChip(
                        selected = allLayers,
                        onClick = { allLayers = true },
                        label = { Text("Entire Project", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForgePurple
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    state.replaceColor(targetColor, replacementColor, tolerance.toInt(), allLayers)
                    onDismiss()
                },
                enabled = matchingPixelsCount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = ForgePurple)
            ) {
                Text("Replace ($matchingPixelsCount px)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
