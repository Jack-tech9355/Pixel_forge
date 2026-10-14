package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drawing.PixelCanvasState
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgePurple
import com.example.ui.theme.ForgeSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReferenceImageDialog(
    state: PixelCanvasState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
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
                        state.referenceBitmap = bitmap
                        state.isReferenceVisible = true
                        state.referenceOffset = Offset.Zero
                        state.referenceScale = 1.0f
                        Toast.makeText(context, "Reference image loaded", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Image, contentDescription = null, tint = ForgeCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Reference Image Overlay",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Display an offline reference overlay over your canvas to guide your drawing. Never included in exports.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (state.referenceBitmap == null) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Reference Photo", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Controls for loaded reference image
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (state.isReferenceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (state.isReferenceVisible) ForgeCyan else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Visible Overlay", fontSize = 13.sp, color = Color.White)
                        }
                        Switch(
                            checked = state.isReferenceVisible,
                            onCheckedChange = { state.isReferenceVisible = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ForgeCyan, checkedTrackColor = ForgePurple)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Opacity: ${(state.referenceOpacity * 100).toInt()}%", fontSize = 12.sp, color = Color.LightGray)
                    Slider(
                        value = state.referenceOpacity,
                        onValueChange = { state.referenceOpacity = it },
                        valueRange = 0.05f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = ForgeCyan, activeTrackColor = ForgeCyan)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Scale: ${String.format("%.1f", state.referenceScale)}x", fontSize = 12.sp, color = Color.LightGray)
                    Slider(
                        value = state.referenceScale,
                        onValueChange = { state.referenceScale = it },
                        valueRange = 0.2f..4.0f,
                        colors = SliderDefaults.colors(thumbColor = ForgeCyan, activeTrackColor = ForgeCyan)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgePurple),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Change Photo", fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = {
                                state.referenceBitmap = null
                                state.isReferenceVisible = false
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFF87171))
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
}
