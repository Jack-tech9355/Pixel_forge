package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectEntity
import com.example.data.repository.ProjectRepository
import com.example.export.PngExporter
import com.example.ui.components.SpriteSheetDialog
import com.example.ui.theme.ForgeCyan
import com.example.ui.theme.ForgePurple
import com.example.ui.theme.ForgeSurface
import com.example.ui.theme.ForgeSurfaceElevated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GalleryScreen(
    repository: ProjectRepository,
    onOpenProject: (String) -> Unit,
    onCreateNewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allProjects by repository.getAllProjectsFlow().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var sortDescending by remember { mutableStateOf(true) }
    var sortByTitle by remember { mutableStateOf(false) }

    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectToRename by remember { mutableStateOf<ProjectEntity?>(null) }

    // Multi-project selection for Sprite Sheet export
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var multiSpriteSheetBitmaps by remember { mutableStateOf<List<Bitmap>?>(null) }

    val filteredProjects = remember(allProjects, searchQuery, sortDescending, sortByTitle) {
        allProjects
            .filter { it.title.contains(searchQuery, ignoreCase = true) }
            .let { list ->
                if (sortByTitle) {
                    if (sortDescending) list.sortedBy { it.title.lowercase() }
                    else list.sortedByDescending { it.title.lowercase() }
                } else {
                    if (sortDescending) list.sortedByDescending { it.updatedAt }
                    else list.sortedBy { it.updatedAt }
                }
            }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = onCreateNewClick,
                    containerColor = ForgePurple,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("gallery_fab_create")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Artwork")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedIds.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.LightGray)
                        }
                        Text(
                            text = "${selectedIds.size} Selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ForgeCyan
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (selectedIds.size == filteredProjects.size) {
                                selectedIds.clear()
                            } else {
                                selectedIds.clear()
                                selectedIds.addAll(filteredProjects.map { it.id })
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = Color.White)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val bitmaps = withContext(Dispatchers.IO) {
                                        selectedIds.mapNotNull { id ->
                                            repository.getProject(id)?.let { proj ->
                                                PngExporter.compositeToBitmap(proj, true)
                                            }
                                        }
                                    }
                                    if (bitmaps.isNotEmpty()) {
                                        multiSpriteSheetBitmaps = bitmaps
                                    } else {
                                        Toast.makeText(context, "Select at least 1 project", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = selectedIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.GridOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sprite Sheet", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                } else {
                    Text(
                        text = "My Gallery",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = ForgeCyan
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isSelectionMode = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = "Multi Sprite Sheet",
                                tint = ForgeCyan
                            )
                        }

                        IconButton(
                            onClick = { sortByTitle = !sortByTitle },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = if (sortByTitle) ForgeCyan else Color.LightGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search artworks...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = ForgeCyan)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForgeCyan,
                    unfocusedBorderColor = Color(0xFF263352),
                    focusedContainerColor = ForgeSurface,
                    unfocusedContainerColor = ForgeSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_search_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isEmpty()) "Your gallery is empty" else "No matching artworks found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to create your first pixel artwork",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProjects, key = { it.id }) { project ->
                        val isChecked = selectedIds.contains(project.id)
                        GalleryItemCard(
                            project = project,
                            isSelectionMode = isSelectionMode,
                            isChecked = isChecked,
                            onToggleCheck = {
                                if (isChecked) selectedIds.remove(project.id)
                                else selectedIds.add(project.id)
                            },
                            onOpen = {
                                if (isSelectionMode) {
                                    if (isChecked) selectedIds.remove(project.id)
                                    else selectedIds.add(project.id)
                                } else {
                                    onOpenProject(project.id)
                                }
                            },
                            onRename = { projectToRename = project },
                            onDuplicate = {
                                coroutineScope.launch {
                                    repository.duplicateProject(project.id)
                                    Toast.makeText(context, "Duplicated ${project.title}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = { projectToDelete = project },
                            onShare = {
                                coroutineScope.launch {
                                    val full = repository.getProject(project.id)
                                    if (full != null) {
                                        val intent = PngExporter.shareArtwork(context, full)
                                        if (intent != null) context.startActivity(intent)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Multi-project Sprite Sheet Dialog
    multiSpriteSheetBitmaps?.let { bitmaps ->
        SpriteSheetDialog(
            customBitmaps = bitmaps,
            customTitle = "PixelForge_MultiSpriteSheet",
            onDismiss = {
                multiSpriteSheetBitmaps = null
                isSelectionMode = false
                selectedIds.clear()
            }
        )
    }

    // Delete Confirmation
    projectToDelete?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            containerColor = ForgeSurface,
            title = { Text("Delete Artwork") },
            text = { Text("Are you sure you want to permanently delete '${proj.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteProject(proj.id)
                            projectToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Rename Dialog
    projectToRename?.let { proj ->
        var renameText by remember { mutableStateOf(proj.title) }
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            containerColor = ForgeSurface,
            title = { Text("Rename Artwork") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("New Title") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        repository.renameProject(proj.id, renameText)
                        projectToRename = null
                    }
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GalleryItemCard(
    project: ProjectEntity,
    isSelectionMode: Boolean = false,
    isChecked: Boolean = false,
    onToggleCheck: () -> Unit = {},
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onOpen() }
            .testTag("gallery_card_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = ForgeSurface),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isChecked) 2.dp else 1.dp,
            color = if (isChecked) ForgeCyan else Color(0xFF263352)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                // Thumbnail
                ArtworkThumbnailBox(
                    thumbnailBase64 = project.thumbnailBase64,
                    modifier = Modifier.fillMaxSize()
                )

                if (isSelectionMode) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggleCheck() },
                        colors = CheckboxDefaults.colors(checkedColor = ForgeCyan),
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${project.width}×${project.height} px • ${project.layerCount} layers",
                        fontSize = 11.sp,
                        color = ForgeCyan
                    )
                }

                if (!isSelectionMode) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(ForgeSurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open in Editor", color = Color.White) },
                                onClick = {
                                    menuExpanded = false
                                    onOpen()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = ForgeCyan) }
                            )
                            DropdownMenuItem(
                                text = { Text("Share PNG", color = Color.White) },
                                onClick = {
                                    menuExpanded = false
                                    onShare()
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = ForgeCyan) }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename", color = Color.White) },
                                onClick = {
                                    menuExpanded = false
                                    onRename()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.LightGray) }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate", color = Color.White) },
                                onClick = {
                                    menuExpanded = false
                                    onDuplicate()
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.LightGray) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color(0xFFF87171)) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF87171)) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(project.updatedAt)),
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

