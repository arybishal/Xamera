package com.bishal.adaptivecamera.lut.repository

import android.content.Context
import android.net.Uri
import com.bishal.adaptivecamera.lut.database.LutDao
import com.bishal.adaptivecamera.lut.database.LutDatabase
import com.bishal.adaptivecamera.lut.database.LutEntity
import com.bishal.adaptivecamera.lut.model.ColorSpace
import com.bishal.adaptivecamera.lut.model.LutColor
import com.bishal.adaptivecamera.lut.model.LutDefinition
import com.bishal.adaptivecamera.lut.model.LutMetadata
import com.bishal.adaptivecamera.lut.model.LutSourceType
import com.bishal.adaptivecamera.lut.model.LutType
import com.bishal.adaptivecamera.lut.model.TransferFunction
import com.bishal.adaptivecamera.lut.presets.BuiltInLutPresets
import com.bishal.adaptivecamera.lut.storage.LutStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Domain item representing a Look-Up Table in the Xamera library.
 */
data class LutItem(
    val id: String,
    val name: String,
    val description: String,
    val sourceType: LutSourceType,
    val originalFileName: String? = null,
    val lutType: LutType = LutType.LUT_3D,
    val dimension: Int = 17,
    val domainMin: LutColor = LutColor.BLACK,
    val domainMax: LutColor = LutColor.WHITE,
    val contentHash: String,
    val storagePath: String? = null,
    val isFavorite: Boolean = false,
    val dateAddedMs: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    val inputColorSpace: ColorSpace = ColorSpace.REC_709,
    val outputColorSpace: ColorSpace = ColorSpace.REC_709,
    val transferFunction: TransferFunction = TransferFunction.REC_709,
    val previewColorArgb: Int? = null
) {
    val isBuiltIn: Boolean
        get() = sourceType == LutSourceType.BUILT_IN

    val sizeFormatted: String
        get() = if (fileSizeBytes > 0) {
            String.format(java.util.Locale.US, "%.1f KB", fileSizeBytes / 1024.0)
        } else {
            "Built-in"
        }

    val dimensionBadge: String
        get() = "${dimension}${if (lutType == LutType.LUT_3D) "³ 3D" else " 1D"}"
}

enum class LutFilter(val label: String) {
    ALL("All"),
    BUILT_IN("Built-in"),
    IMPORTED("Imported"),
    FAVORITES("Favorites")
}

enum class LutSort(val label: String) {
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First")
}

interface LutRepository {
    val allLutsFlow: Flow<List<LutItem>>
    val selectedLutIdFlow: StateFlow<String?>

    suspend fun initializeBuiltInPresets()
    suspend fun getLutById(id: String): LutItem?
    suspend fun loadLutDefinition(id: String): LutDefinition?
    suspend fun importLutFromUri(uri: Uri): Result<LutItem>
    suspend fun toggleFavorite(id: String)
    suspend fun deleteLut(id: String): Result<Unit>
    suspend fun selectLut(id: String?)
}

class LutRepositoryImpl(
    private val context: Context,
    private val lutDao: LutDao = LutDatabase.getInstance(context).lutDao(),
    private val storageManager: LutStorageManager = LutStorageManager(context),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : LutRepository {

    private val _selectedLutIdFlow = MutableStateFlow<String?>(null)
    override val selectedLutIdFlow: StateFlow<String?> = _selectedLutIdFlow.asStateFlow()

    init {
        coroutineScope.launch {
            initializeBuiltInPresets()
        }
    }

    override val allLutsFlow: Flow<List<LutItem>> = lutDao.getAllLutsFlow().map { entities ->
        entities.map { it.toLutItem() }
    }

    override suspend fun initializeBuiltInPresets() = withContext(Dispatchers.IO) {
        val existingBuiltIns = lutDao.getBuiltInLuts().map { it.id }.toSet()
        val newEntities = mutableListOf<LutEntity>()

        for (preset in BuiltInLutPresets.PRESET_INFOS) {
            if (!existingBuiltIns.contains(preset.id)) {
                newEntities.add(
                    LutEntity(
                        id = preset.id,
                        name = preset.title,
                        description = preset.description,
                        sourceType = LutSourceType.BUILT_IN.name,
                        originalFileName = null,
                        lutType = LutType.LUT_3D.name,
                        dimension = 17,
                        domainMinR = 0f,
                        domainMinG = 0f,
                        domainMinB = 0f,
                        domainMaxR = 1f,
                        domainMaxG = 1f,
                        domainMaxB = 1f,
                        contentHash = "hash_${preset.id}",
                        storagePath = null,
                        isFavorite = false,
                        dateAddedMs = 0L, // Fixed baseline time for built-in presets
                        fileSizeBytes = 0L,
                        inputColorSpace = preset.inputColorSpace.id,
                        outputColorSpace = preset.outputColorSpace.id,
                        transferFunction = preset.transferFunction.id
                    )
                )
            }
        }

        if (newEntities.isNotEmpty()) {
            lutDao.insertAll(newEntities)
        }
    }

    override suspend fun getLutById(id: String): LutItem? = withContext(Dispatchers.IO) {
        lutDao.getLutById(id)?.toLutItem()
    }

    override suspend fun loadLutDefinition(id: String): LutDefinition? = withContext(Dispatchers.IO) {
        val entity = lutDao.getLutById(id) ?: return@withContext null
        if (entity.isBuiltIn) {
            BuiltInLutPresets.createPresetDefinition(entity.id, size = entity.dimension)
        } else {
            val path = entity.storagePath ?: return@withContext null
            storageManager.loadLutDefinition(path)
        }
    }

    override suspend fun importLutFromUri(uri: Uri): Result<LutItem> = withContext(Dispatchers.IO) {
        val payloadResult = storageManager.importFromUri(uri)
        if (payloadResult.isFailure) {
            return@withContext Result.failure(payloadResult.exceptionOrNull() ?: Exception("Unknown import failure"))
        }

        val payload = payloadResult.getOrThrow()

        // Deduplication Check
        val existingByHash = lutDao.getLutByContentHash(payload.contentHash)
        if (existingByHash != null) {
            return@withContext Result.success(existingByHash.toLutItem())
        }

        val meta = payload.definition.metadata
        val entityId = "imported_${payload.contentHash.take(12)}"
        val entity = LutEntity(
            id = entityId,
            name = meta.title,
            description = "Imported 3D LUT (${meta.size}x${meta.size}x${meta.size}) from ${payload.originalFilename}",
            sourceType = LutSourceType.IMPORTED.name,
            originalFileName = payload.originalFilename,
            lutType = meta.type.name,
            dimension = meta.size,
            domainMinR = meta.domainMin.r,
            domainMinG = meta.domainMin.g,
            domainMinB = meta.domainMin.b,
            domainMaxR = meta.domainMax.r,
            domainMaxG = meta.domainMax.g,
            domainMaxB = meta.domainMax.b,
            contentHash = payload.contentHash,
            storagePath = payload.storagePath,
            isFavorite = false,
            dateAddedMs = System.currentTimeMillis(),
            fileSizeBytes = payload.fileSizeBytes,
            inputColorSpace = meta.inputColorSpace.id,
            outputColorSpace = meta.outputColorSpace.id,
            transferFunction = meta.transferFunction.id
        )

        lutDao.insertLut(entity)
        Result.success(entity.toLutItem())
    }

    override suspend fun toggleFavorite(id: String) = withContext(Dispatchers.IO) {
        val entity = lutDao.getLutById(id) ?: return@withContext
        val newFav = !entity.isFavorite
        lutDao.updateFavorite(id, newFav)
    }

    override suspend fun deleteLut(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val entity = lutDao.getLutById(id)
            ?: return@withContext Result.failure(IllegalArgumentException("LUT not found: $id"))

        if (entity.isBuiltIn) {
            return@withContext Result.failure(IllegalStateException("Built-in presets cannot be deleted"))
        }

        entity.storagePath?.let { storageManager.deleteLutFile(it) }
        val rows = lutDao.deleteImportedLut(id)

        if (_selectedLutIdFlow.value == id) {
            _selectedLutIdFlow.value = null
        }

        if (rows > 0) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Could not delete record from database"))
        }
    }

    override suspend fun selectLut(id: String?) {
        _selectedLutIdFlow.value = id
    }

    private fun LutEntity.toLutItem(): LutItem {
        val builtInInfo = BuiltInLutPresets.PRESET_INFOS.firstOrNull { it.id == id }
        val src = if (sourceType == LutSourceType.BUILT_IN.name) LutSourceType.BUILT_IN else LutSourceType.IMPORTED
        val lType = if (lutType == LutType.LUT_1D.name) LutType.LUT_1D else LutType.LUT_3D

        return LutItem(
            id = id,
            name = name,
            description = description,
            sourceType = src,
            originalFileName = originalFileName,
            lutType = lType,
            dimension = dimension,
            domainMin = LutColor(domainMinR, domainMinG, domainMinB),
            domainMax = LutColor(domainMaxR, domainMaxG, domainMaxB),
            contentHash = contentHash,
            storagePath = storagePath,
            isFavorite = isFavorite,
            dateAddedMs = dateAddedMs,
            fileSizeBytes = fileSizeBytes,
            inputColorSpace = ColorSpace.fromId(inputColorSpace),
            outputColorSpace = ColorSpace.fromId(outputColorSpace),
            transferFunction = TransferFunction.fromId(transferFunction),
            previewColorArgb = builtInInfo?.previewColorArgb ?: 0xFF00BCD4.toInt()
        )
    }
}
