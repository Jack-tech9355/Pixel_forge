package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.BuiltInTemplates
import com.example.data.repository.ProjectRepository
import com.example.data.serialization.ProjectSerializer
import com.example.model.PixelLayer
import com.example.model.PixelProject
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgeEmerald
import com.example.ui.theme.ForgePurple
import com.example.ui.theme.ForgeSurface
import com.example.ui.theme.ForgeSurfaceElevated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun CommunityShowcaseScreen(
    repository: ProjectRepository,
    onOpenProject: (PixelProject) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingExportProject by remember { mutableStateOf<PixelProject?>(null) }

    // Export JSON project bundle
    val exportBundleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingExportProject != null) {
            scope.launch {
                try {
                    val json = ProjectSerializer.toJson(pendingExportProject!!)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }
                    Toast.makeText(context, "Exported project backup bundle!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Import JSON project bundle
    val importBundleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader(Charsets.UTF_8).readText()
                        }
                    }
                    if (json != null) {
                        val imported = ProjectSerializer.fromJson(json)
                        val copyWithNewId = imported.copy(
                            id = UUID.randomUUID().toString(),
                            title = "${imported.title} (Imported)"
                        )
                        repository.saveProject(copyWithNewId)
                        Toast.makeText(context, "Imported '${copyWithNewId.title}' successfully!", Toast.LENGTH_SHORT).show()
                        onOpenProject(copyWithNewId)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Invalid project bundle: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Community & Cloud",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Showcase, offline backup bundles & private sync",
                    fontSize = 13.sp,
                    color = Color.LightGray
                )
            }
            Icon(Icons.Default.Explore, contentDescription = null, tint = ForgeCyan, modifier = Modifier.size(28.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Offline-First Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ForgeSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = ForgeEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("100% Offline-First Architecture", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "All your artwork, animation frames, and custom palettes reside safely in device storage. No account, sign-in, or internet connection is required to draw or export.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            importBundleLauncher.launch(arrayOf("application/json", "text/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgePurple),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Bundle", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val recent = repository.getMostRecentProject()
                                if (recent != null) {
                                    pendingExportProject = recent
                                    val sanitized = recent.title.replace(Regex("[^a-zA-Z0-9_]"), "_")
                                    exportBundleLauncher.launch("${sanitized}_backup.json")
                                } else {
                                    Toast.makeText(context, "No saved projects yet", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Backup", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Community Showcase section
        Text(
            "Community Showcase & Starter Templates",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Tap any creation to open and remix it locally in the Pixel Studio.",
            fontSize = 12.sp,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(12.dp))

        val templates = BuiltInTemplates.ALL
        for (tmpl in templates) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        val proj = PixelProject(
                            id = UUID.randomUUID().toString(),
                            title = tmpl.title,
                            width = tmpl.width,
                            height = tmpl.height,
                            layers = mutableListOf(
                                PixelLayer(name = "Base Layer", pixels = tmpl.pixelGenerator())
                            )
                        )
                        scope.launch {
                            repository.saveProject(proj)
                            onOpenProject(proj)
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = ForgeSurfaceElevated),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tmpl.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text(tmpl.description, fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${tmpl.width}×${tmpl.height} px • ${tmpl.category}", fontSize = 10.sp, color = ForgeCyan)
                    }
                    Button(
                        onClick = {
                            val proj = PixelProject(
                                id = UUID.randomUUID().toString(),
                                title = tmpl.title,
                                width = tmpl.width,
                                height = tmpl.height,
                                layers = mutableListOf(
                                    PixelLayer(name = "Base Layer", pixels = tmpl.pixelGenerator())
                                )
                            )
                            scope.launch {
                                repository.saveProject(proj)
                                onOpenProject(proj)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgePurple),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Remix", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
