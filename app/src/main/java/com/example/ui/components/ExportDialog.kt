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
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.export.GifEncoder
import com.example.export.PngExporter
import com.example.model.PixelProject
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgePurple
import com.example.ui.theme.ForgeSurface
import com.example.ui.theme.ForgeSurfaceElevated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportDialog(
    project: PixelProject,
    onSaveToStorage: (scale: Int, includeBg: Boolean) -> Unit,
    onOpenSpriteSheet: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: PNG, 1: Animated GIF, 2: Sprite Sheet

    var selectedScale by remember { mutableIntStateOf(8) }
    var includeBackground by remember { mutableStateOf(true) }
    var gifFps by remember { mutableFloatStateOf(8f) }
    var isExportingGif by remember { mutableStateOf(false) }

    val scales = listOf(1, 2, 4, 8, 16, 32)
    val outputWidth = project.width * selectedScale
    val outputHeight = project.height * selectedScale

    val saveGifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/gif")
    ) { uri: Uri? ->
        if (uri != null) {
            isExportingGif = true
            scope.launch {
                try {
                    val frameBitmaps = project.frames.map { f ->
                        val p = project.copy(layers = f.layers)
                        val bmp = PngExporter.compositeToBitmap(p, includeBackground)
                        PngExporter.createScaledPixelBitmap(bmp, selectedScale.coerceAtMost(8))
                    }
                    val success = withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            GifEncoder.encodeBitmaps(frameBitmaps, gifFps.toInt(), out)
                        } ?: false
                    }
                    if (success) {
                        Toast.makeText(context, "Animated GIF exported successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "GIF export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isExportingGif = false
                    onDismiss()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeSurface,
        title = {
            Column {
                Text(
                    "Export Studio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = ForgeSurfaceElevated,
                    contentColor = ForgeCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ForgeCyan
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("PNG Artwork", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Animated GIF", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Animation, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = {
                            onDismiss()
                            onOpenSpriteSheet()
                        },
                        text = { Text("Sprite Sheet", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (selectedTab == 0) {
                    // PNG Artwork Export Content
                    Text(
                        "Crisp nearest-neighbor PNG pixel art export with zero blur.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Scale Factor:", fontSize = 12.sp, color = ForgeCyan, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (scale in scales) {
                            val isSelected = selectedScale == scale
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ForgePurple else ForgeSurfaceElevated)
                                    .border(1.dp, if (isSelected) ForgeCyan else Color(0xFF263352), RoundedCornerShape(8.dp))
                                    .clickable { selectedScale = scale }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "${scale}x",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Output dimensions badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ForgeSurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Output Size:", color = Color.Gray, fontSize = 12.sp)
                            Text("${outputWidth} × ${outputHeight} px", color = ForgeCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Include background color", fontSize = 12.sp, color = Color.LightGray)
                    }
                } else if (selectedTab == 1) {
                    // Animated GIF Export Content
                    val frameCount = project.frames.size
                    Text(
                        "Export $frameCount frames as a looping standard Animated GIF89a.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Playback Speed: ${gifFps.toInt()} FPS", fontSize = 12.sp, color = ForgeCyan, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = gifFps,
                        onValueChange = { gifFps = it },
                        valueRange = 1f..24f,
                        steps = 22,
                        colors = SliderDefaults.colors(thumbColor = ForgeCyan, activeTrackColor = ForgeCyan)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("GIF Resolution Scale:", fontSize = 12.sp, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 4, 8).forEach { s ->
                            FilterChip(
                                selected = selectedScale == s,
                                onClick = { selectedScale = s },
                                label = { Text("${s}x", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ForgePurple)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Include background color", fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF080B18))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = Color(0xFF080B18), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onSaveToStorage(selectedScale, includeBackground)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgePurple),
                        modifier = Modifier.testTag("save_png_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save PNG", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else if (selectedTab == 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isExportingGif = true
                            scope.launch {
                                shareGif(context, project, selectedScale.coerceAtMost(8), gifFps.toInt(), includeBackground)
                                isExportingGif = false
                                onDismiss()
                            }
                        },
                        enabled = !isExportingGif,
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF080B18))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share GIF", color = Color(0xFF080B18), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val sanitized = project.title.replace(Regex("[^a-zA-Z0-9_]"), "_")
                            saveGifLauncher.launch("${sanitized}.gif")
                        },
                        enabled = !isExportingGif,
                        colors = ButtonDefaults.buttonColors(containerColor = ForgePurple)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isExportingGif) "Encoding..." else "Save GIF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
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

private suspend fun shareGif(
    context: Context,
    project: PixelProject,
    scale: Int,
    fps: Int,
    includeBg: Boolean
) = withContext(Dispatchers.IO) {
    try {
        val cacheDir = File(context.cacheDir, "exports")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val file = File(cacheDir, "${project.title}.gif")

        val frameBitmaps = project.frames.map { f ->
            val p = project.copy(layers = f.layers)
            val bmp = PngExporter.compositeToBitmap(p, includeBg)
            PngExporter.createScaledPixelBitmap(bmp, scale)
        }

        FileOutputStream(file).use { out ->
            GifEncoder.encodeBitmaps(frameBitmaps, fps, out)
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/gif"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${project.title} (GIF)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Animated GIF"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
