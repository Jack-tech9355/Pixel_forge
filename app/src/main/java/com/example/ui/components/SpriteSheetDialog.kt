package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.export.PngExporter
import com.example.export.SpriteSheetExporter
import com.example.model.PixelProject
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgePurple
import com.example.ui.theme.ForgeSurface
import com.example.ui.theme.ForgeSurfaceElevated
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil

@Composable
fun SpriteSheetDialog(
    project: PixelProject? = null,
    customBitmaps: List<Bitmap>? = null,
    customTitle: String = "Sprite Sheet",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var columns by remember { mutableFloatStateOf(4f) }
    var spacing by remember { mutableFloatStateOf(2f) }
    var margin by remember { mutableFloatStateOf(4f) }
    var scale by remember { mutableIntStateOf(2) }
    var includeBg by remember { mutableStateOf(false) }

    // Collect frames from project or custom bitmaps
    val frameBitmaps = remember(project, customBitmaps, includeBg) {
        if (customBitmaps != null) {
            customBitmaps
        } else if (project != null) {
            if (project.frames.isNotEmpty()) {
                project.frames.map { frame ->
                    val p = project.copy(layers = frame.layers)
                    PngExporter.compositeToBitmap(p, includeBg)
                }
            } else {
                listOf(PngExporter.compositeToBitmap(project, includeBg))
            }
        } else {
            emptyList()
        }
    }

    val count = frameBitmaps.size
    val cols = columns.toInt().coerceIn(1, maxOf(1, count))
    val rows = ceil(count.toDouble() / cols).toInt()
    val baseW = if (frameBitmaps.isNotEmpty()) frameBitmaps[0].width else 32
    val baseH = if (frameBitmaps.isNotEmpty()) frameBitmaps[0].height else 32
    val spriteW = baseW * scale
    val spriteH = baseH * scale
    val totalW = margin.toInt() * 2 + cols * spriteW + (cols - 1) * spacing.toInt()
    val totalH = margin.toInt() * 2 + rows * spriteH + (rows - 1) * spacing.toInt()

    val title = project?.title ?: customTitle

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        if (uri != null) {
            val config = SpriteSheetExporter.SpriteSheetConfig(
                columns = cols,
                spacing = spacing.toInt(),
                margin = margin.toInt(),
                scale = scale,
                includeBackground = includeBg
            )
            val sheet = SpriteSheetExporter.createFramesSpriteSheet(frameBitmaps, config)
            if (sheet != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        SpriteSheetExporter.exportToStream(sheet, out)
                    }
                    Toast.makeText(context, "Sprite Sheet exported successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GridOn, contentDescription = null, tint = ForgeCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Export Sprite Sheet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Export all $count sprite frames into a single compact PNG sprite sheet.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Dimension and Sheet Preview Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ForgeSurfaceElevated)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grid Layout:", fontSize = 12.sp, color = Color.Gray)
                            Text("$cols cols × $rows rows ($count items)", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sheet Dimensions:", fontSize = 12.sp, color = Color.Gray)
                            Text("${totalW} × ${totalH} px", fontSize = 12.sp, color = ForgeCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Columns slider
                Text("Columns: $cols", fontSize = 12.sp, color = Color.LightGray)
                Slider(
                    value = columns,
                    onValueChange = { columns = it },
                    valueRange = 1f..maxOf(1f, minOf(16f, count.toFloat())),
                    steps = (minOf(16, count) - 1).coerceAtLeast(0),
                    colors = SliderDefaults.colors(thumbColor = ForgeCyan, activeTrackColor = ForgeCyan)
                )

                // Spacing slider
                Text("Sprite Padding: ${spacing.toInt()} px", fontSize = 12.sp, color = Color.LightGray)
                Slider(
                    value = spacing,
                    onValueChange = { spacing = it },
                    valueRange = 0f..16f,
                    colors = SliderDefaults.colors(thumbColor = ForgeCyan, activeTrackColor = ForgeCyan)
                )

                // Scale row
                Text("Sprite Scale:", fontSize = 12.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 2, 4, 8).forEach { s ->
                        FilterChip(
                            selected = scale == s,
                            onClick = { scale = s },
                            label = { Text("${s}x", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ForgePurple)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = includeBg,
                        onCheckedChange = { includeBg = it },
                        colors = CheckboxDefaults.colors(checkedColor = ForgePurple)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Include Background", fontSize = 12.sp, color = Color.LightGray)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Share button
                Button(
                    onClick = {
                        val config = SpriteSheetExporter.SpriteSheetConfig(
                            columns = cols,
                            spacing = spacing.toInt(),
                            margin = margin.toInt(),
                            scale = scale,
                            includeBackground = includeBg
                        )
                        val sheet = SpriteSheetExporter.createFramesSpriteSheet(frameBitmaps, config)
                        if (sheet != null) {
                            shareSpriteSheet(context, title, sheet)
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Save button
                Button(
                    onClick = {
                        val sanitized = title.replace(Regex("[^a-zA-Z0-9_]"), "_")
                        saveDocumentLauncher.launch("${sanitized}_spritesheet_${totalW}x${totalH}.png")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgePurple)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save PNG", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

private fun shareSpriteSheet(context: Context, title: String, sheet: Bitmap) {
    try {
        val cacheDir = File(context.cacheDir, "exports")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val file = File(cacheDir, "${title}_spritesheet.png")
        FileOutputStream(file).use { out ->
            sheet.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "$title Sprite Sheet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Sprite Sheet"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
