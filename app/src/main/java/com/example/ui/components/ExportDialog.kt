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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.export.PngExporter
import com.example.model.PixelProject
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgePurple
import com.example.ui.theme.ForgeSurface
import com.example.ui.theme.ForgeSurfaceElevated

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportDialog(
    project: PixelProject,
    onSaveToStorage: (scale: Int, includeBg: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedScale by remember { mutableIntStateOf(8) }
    var includeBackground by remember { mutableStateOf(true) }

    val scales = listOf(1, 2, 4, 8, 16, 32)
    val outputWidth = project.width * selectedScale
    val outputHeight = project.height * selectedScale

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeSurface,
        title = {
            Text(
                "Export PNG Artwork",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Choose scale factor for crisp nearest-neighbor export (no blur).",
                    fontSize = 13.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Resolution Scale:",
                    style = MaterialTheme.typography.titleSmall,
                    color = ForgeCyan,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (scale in scales) {
                        val isSelected = selectedScale == scale
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ForgePurple else ForgeSurfaceElevated)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) ForgeCyan else Color(0xFF263352),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedScale = scale }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "${scale}x",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dimension feedback badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ForgeSurfaceElevated)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Output Size:", color = Color.Gray, fontSize = 13.sp)
                        Text(
                            "${outputWidth} × ${outputHeight} px",
                            color = ForgeCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transparent background toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { includeBackground = !includeBackground }
                ) {
                    Checkbox(
                        checked = includeBackground,
                        onCheckedChange = { includeBackground = it },
                        colors = CheckboxDefaults.colors(checkedColor = ForgePurple)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Include canvas background color",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Share button
                Button(
                    onClick = {
                        val shareIntent = PngExporter.shareArtwork(
                            context = context,
                            project = project,
                            scale = selectedScale,
                            includeBackground = includeBackground
                        )
                        if (shareIntent != null) {
                            context.startActivity(shareIntent)
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan),
                    modifier = Modifier.testTag("share_png_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF080B18)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", color = Color(0xFF080B18), fontWeight = FontWeight.Bold)
                }

                // Save to Storage button
                Button(
                    onClick = {
                        onSaveToStorage(selectedScale, includeBackground)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgePurple),
                    modifier = Modifier.testTag("save_png_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save PNG", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
