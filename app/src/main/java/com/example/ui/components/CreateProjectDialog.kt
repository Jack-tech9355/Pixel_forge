package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgePurple
import com.example.ui.theme.ForgeSurface
import com.example.ui.theme.ForgeSurfaceElevated

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateProjectDialog(
    initialSize: Int = 32,
    onCreate: (title: String, width: Int, height: Int, bgColor: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("New Artwork") }
    var selectedWidth by remember { mutableIntStateOf(initialSize) }
    var selectedHeight by remember { mutableIntStateOf(initialSize) }
    var isCustom by remember { mutableStateOf(false) }

    var customWText by remember { mutableStateOf("32") }
    var customHText by remember { mutableStateOf("32") }

    var selectedBgColor by remember { mutableStateOf<Int?>(null) } // null = transparent

    val standardSizes = listOf(16, 32, 48, 64, 96, 128)

    val bgOptions = listOf(
        Pair("Transparent", null),
        Pair("Deep Navy", 0xFF080B18.toInt()),
        Pair("Retro Slate", 0xFF19233B.toInt()),
        Pair("White", 0xFFFFFFFF.toInt()),
        Pair("Game Boy", 0xFF0F380F.toInt())
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeSurface,
        title = {
            Text(
                "Create Canvas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Artwork Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForgeCyan,
                        unfocusedBorderColor = ForgeSurfaceElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Canvas Dimensions:",
                    style = MaterialTheme.typography.titleSmall,
                    color = ForgeCyan,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (size in standardSizes) {
                        val isSelected = !isCustom && selectedWidth == size && selectedHeight == size
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ForgePurple else ForgeSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isSelected) ForgeCyan else Color(0xFF263352),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    isCustom = false
                                    selectedWidth = size
                                    selectedHeight = size
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "${size}×${size}",
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Custom toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCustom) ForgePurple else ForgeSurfaceElevated)
                            .border(
                                1.dp,
                                if (isCustom) ForgeCyan else Color(0xFF263352),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { isCustom = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "Custom",
                            color = if (isCustom) Color.White else Color.LightGray,
                            fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }

                if (isCustom) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customWText,
                            onValueChange = {
                                customWText = it.filter { ch -> ch.isDigit() }.take(3)
                                selectedWidth = customWText.toIntOrNull()?.coerceIn(8, 128) ?: 32
                            },
                            label = { Text("Width (8-128)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customHText,
                            onValueChange = {
                                customHText = it.filter { ch -> ch.isDigit() }.take(3)
                                selectedHeight = customHText.toIntOrNull()?.coerceIn(8, 128) ?: 32
                            },
                            label = { Text("Height (8-128)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Background:",
                    style = MaterialTheme.typography.titleSmall,
                    color = ForgeCyan,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for ((name, colorVal) in bgOptions) {
                        val isBgSelected = selectedBgColor == colorVal
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isBgSelected) ForgePurple.copy(alpha = 0.3f) else ForgeSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isBgSelected) ForgeCyan else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedBgColor = colorVal }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (colorVal != null) Color(colorVal) else Color(0xFF64748B))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    name,
                                    fontSize = 12.sp,
                                    color = if (isBgSelected) Color.White else Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.trim().ifEmpty { "New Artwork" }
                    val finalW = if (isCustom) (customWText.toIntOrNull() ?: 32).coerceIn(8, 128) else selectedWidth
                    val finalH = if (isCustom) (customHText.toIntOrNull() ?: 32).coerceIn(8, 128) else selectedHeight
                    onCreate(finalTitle, finalW, finalH, selectedBgColor)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForgePurple),
                modifier = Modifier.testTag("confirm_create_project_button")
            ) {
                Text("Create Canvas", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
