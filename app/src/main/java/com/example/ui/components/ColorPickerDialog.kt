package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.BuiltInPalettes
import com.example.model.PixelPalette
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgePurple
import com.example.ui.theme.ForgeSurface
import com.example.ui.theme.ForgeSurfaceElevated

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    initialColor: Int,
    activePalette: PixelPalette,
    onPaletteSelect: (PixelPalette) -> Unit,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var red by remember { mutableFloatStateOf(AndroidColor.red(initialColor).toFloat()) }
    var green by remember { mutableFloatStateOf(AndroidColor.green(initialColor).toFloat()) }
    var blue by remember { mutableFloatStateOf(AndroidColor.blue(initialColor).toFloat()) }
    var alpha by remember { mutableFloatStateOf(AndroidColor.alpha(initialColor).toFloat()) }

    val currentColorInt = AndroidColor.argb(
        alpha.toInt().coerceIn(0, 255),
        red.toInt().coerceIn(0, 255),
        green.toInt().coerceIn(0, 255),
        blue.toInt().coerceIn(0, 255)
    )

    var hexText by remember {
        mutableStateOf(String.format("%08X", currentColorInt))
    }
    var hexError by remember { mutableStateOf(false) }

    fun updateFromInt(colorInt: Int) {
        red = AndroidColor.red(colorInt).toFloat()
        green = AndroidColor.green(colorInt).toFloat()
        blue = AndroidColor.blue(colorInt).toFloat()
        alpha = AndroidColor.alpha(colorInt).toFloat()
        hexText = String.format("%08X", colorInt)
        hexError = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeSurface,
        title = {
            Text(
                "Color Studio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Color comparison preview box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(currentColorInt))
                            .border(2.dp, ForgePurple, RoundedCornerShape(12.dp))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(initialColor))
                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Original", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hex input
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        hexText = input.uppercase().filter { it in "0123456789ABCDEF" }.take(8)
                        if (hexText.length == 8) {
                            try {
                                val parsed = hexText.toLong(16).toInt()
                                updateFromInt(parsed)
                                hexError = false
                            } catch (_: Exception) {
                                hexError = true
                            }
                        } else if (hexText.length == 6) {
                            try {
                                val parsed = ("FF$hexText").toLong(16).toInt()
                                updateFromInt(parsed)
                                hexError = false
                            } catch (_: Exception) {
                                hexError = true
                            }
                        }
                    },
                    label = { Text("HEX Color (AARRGGBB)") },
                    isError = hexError,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForgeCyan,
                        unfocusedBorderColor = ForgeSurfaceElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hex_color_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // RGB and Alpha Sliders
                ColorSlider(label = "Red (${red.toInt()})", value = red, onValueChange = {
                    red = it
                    hexText = String.format("%08X", AndroidColor.argb(alpha.toInt(), red.toInt(), green.toInt(), blue.toInt()))
                }, trackColor = Color(0xFFEF4444))

                ColorSlider(label = "Green (${green.toInt()})", value = green, onValueChange = {
                    green = it
                    hexText = String.format("%08X", AndroidColor.argb(alpha.toInt(), red.toInt(), green.toInt(), blue.toInt()))
                }, trackColor = Color(0xFF22C55E))

                ColorSlider(label = "Blue (${blue.toInt()})", value = blue, onValueChange = {
                    blue = it
                    hexText = String.format("%08X", AndroidColor.argb(alpha.toInt(), red.toInt(), green.toInt(), blue.toInt()))
                }, trackColor = Color(0xFF3B82F6))

                ColorSlider(label = "Alpha (${alpha.toInt()})", value = alpha, onValueChange = {
                    alpha = it
                    hexText = String.format("%08X", AndroidColor.argb(alpha.toInt(), red.toInt(), green.toInt(), blue.toInt()))
                }, trackColor = Color.LightGray)

                Spacer(modifier = Modifier.height(16.dp))

                // Active Palette Swatches
                Text(
                    text = "${activePalette.name} Swatches",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Transparent option
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .clickable { updateFromInt(0) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("∅", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    for (c in activePalette.colors) {
                        val isSelected = currentColorInt == c
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) ForgeCyan else Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable { updateFromInt(c) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (AndroidColor.alpha(c) > 0 && (AndroidColor.red(c) * 0.299 + AndroidColor.green(c) * 0.587 + AndroidColor.blue(c) * 0.114) > 150) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Palette Switcher list
                Text(
                    text = "Preset Palettes",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (pal in BuiltInPalettes.ALL_PRESETS) {
                        val isCurrentPal = pal.id == activePalette.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrentPal) ForgePurple.copy(alpha = 0.25f) else ForgeSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isCurrentPal) ForgePurple else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onPaletteSelect(pal) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                pal.name,
                                fontSize = 12.sp,
                                color = if (isCurrentPal) ForgeCyan else Color.LightGray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(currentColorInt)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForgePurple),
                modifier = Modifier.testTag("apply_color_button")
            ) {
                Text("Select Color", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    trackColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 12.sp, color = Color.LightGray)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = trackColor,
                activeTrackColor = trackColor,
                inactiveTrackColor = ForgeSurfaceElevated
            )
        )
    }
}
