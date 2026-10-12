package com.example.data.repository

import android.content.Context
import com.example.data.BuiltInTemplates
import com.example.data.local.PixelForgeDatabase
import com.example.data.local.ProjectEntity
import com.example.data.serialization.ProjectSerializer
import com.example.export.PngExporter
import com.example.model.PixelLayer
import com.example.model.PixelProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ProjectRepository(private val context: Context) {

    private val database = PixelForgeDatabase.getDatabase(context)
    private val projectDao = database.projectDao()
    private val projectsDir = File(context.filesDir, "projects").apply {
        if (!exists()) mkdirs()
    }

    fun getAllProjectsFlow(): Flow<List<ProjectEntity>> = projectDao.getAllProjectsFlow()

    suspend fun getAllProjects(): List<ProjectEntity> = withContext(Dispatchers.IO) {
        projectDao.getAllProjects()
    }

    suspend fun getProject(id: String): PixelProject? = withContext(Dispatchers.IO) {
        val file = File(projectsDir, "$id.json")
        val loaded = ProjectSerializer.loadFromFile(file)
        if (loaded != null) {
            val entity = projectDao.getProjectById(id)
            if (entity != null) {
                loaded.title = entity.title
            }
        }
        loaded
    }

    suspend fun saveProject(project: PixelProject): Boolean = withContext(Dispatchers.IO) {
        try {
            project.updatedAt = System.currentTimeMillis()
            val file = File(projectsDir, "${project.id}.json")
            val fileSaved = ProjectSerializer.saveToFileAtomic(project, file)
            if (!fileSaved) return@withContext false

            val thumbnail = PngExporter.generateThumbnailBase64(project)
            project.previewBase64 = thumbnail

            val entity = ProjectEntity(
                id = project.id,
                title = project.title,
                width = project.width,
                height = project.height,
                createdAt = project.createdAt,
                updatedAt = project.updatedAt,
                layerCount = project.layers.size,
                thumbnailBase64 = thumbnail
            )
            projectDao.insertOrUpdate(entity)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteProject(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(projectsDir, "$id.json")
            if (file.exists()) file.delete()
            projectDao.deleteProject(id)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun duplicateProject(id: String): PixelProject? = withContext(Dispatchers.IO) {
        val original = getProject(id) ?: return@withContext null
        val newId = UUID.randomUUID().toString()
        val duplicatedLayers = original.layers.map { layer ->
            layer.copy(
                id = UUID.randomUUID().toString(),
                pixels = layer.pixels.clone()
            )
        }.toMutableList()

        val duplicated = PixelProject(
            id = newId,
            title = "${original.title} (Copy)",
            width = original.width,
            height = original.height,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            layers = duplicatedLayers,
            selectedColor = original.selectedColor,
            paletteId = original.paletteId,
            backgroundColor = original.backgroundColor
        )
        saveProject(duplicated)
        duplicated
    }

    suspend fun renameProject(id: String, newTitle: String): Boolean = withContext(Dispatchers.IO) {
        val project = getProject(id) ?: return@withContext false
        project.title = newTitle.trim().ifEmpty { "Untitled Artwork" }
        saveProject(project)
    }

    suspend fun getMostRecentProject(): PixelProject? = withContext(Dispatchers.IO) {
        val all = projectDao.getAllProjects()
        val first = all.firstOrNull() ?: return@withContext null
        getProject(first.id)
    }

    suspend fun seedInitialProjectsIfEmpty() = withContext(Dispatchers.IO) {
        val count = projectDao.getAllProjects().size
        if (count == 0) {
            // Seed first 2 templates as user editable projects
            val templates = BuiltInTemplates.ALL.take(2)
            for (tmpl in templates) {
                val proj = PixelProject(
                    id = UUID.randomUUID().toString(),
                    title = tmpl.title,
                    width = tmpl.width,
                    height = tmpl.height,
                    layers = mutableListOf(
                        PixelLayer(
                            name = "Base Layer",
                            pixels = tmpl.pixelGenerator()
                        )
                    )
                )
                saveProject(proj)
            }
        }
    }

    suspend fun clearAllData(): Boolean = withContext(Dispatchers.IO) {
        try {
            projectsDir.listFiles()?.forEach { it.delete() }
            projectDao.clearAll()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getStorageUsageBytes(): Long = withContext(Dispatchers.IO) {
        var total = 0L
        projectsDir.listFiles()?.forEach { total += it.length() }
        total
    }
}
