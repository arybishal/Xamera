package com.bishal.adaptivecamera.lut.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.bishal.adaptivecamera.lut.model.LutDefinition
import com.bishal.adaptivecamera.lut.model.LutValidationResult
import com.bishal.adaptivecamera.lut.parser.CubeLutParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

data class LutImportPayload(
    val originalFilename: String,
    val contentHash: String,
    val fileSizeBytes: Long,
    val definition: LutDefinition,
    val storagePath: String
)

/**
 * Manages file persistence, SHA-256 integrity verification, and Storage Access Framework
 * imports for Look-Up Table (.cube) files stored in app-managed internal storage.
 */
class LutStorageManager(private val context: Context) {

    private val lutsDirectory: File
        get() {
            val dir = File(context.filesDir, "luts")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Imports and parses a .cube LUT file from a Storage Access Framework [uri].
     * Enforces a 20 MB memory safety guard and computes SHA-256 content hash.
     */
    suspend fun importFromUri(uri: Uri): Result<LutImportPayload> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // 1. Resolve Display Name
            var displayName = "imported.cube"
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                }
            } catch (_: Exception) {
                uri.lastPathSegment?.let { displayName = it }
            }

            // 2. Read bytes with safety limit (20 MB maximum)
            val maxBytes = 20 * 1024 * 1024
            val stream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalArgumentException("Cannot open stream for URI: $uri"))

            val bytes = stream.use { input ->
                val buffer = java.io.ByteArrayOutputStream()
                val temp = ByteArray(8192)
                var read = input.read(temp)
                var total = 0
                while (read != -1) {
                    total += read
                    if (total > maxBytes) {
                        return@withContext Result.failure(IllegalStateException("File exceeds maximum allowed size (20 MB)"))
                    }
                    buffer.write(temp, 0, read)
                    read = input.read(temp)
                }
                buffer.toByteArray()
            }

            if (bytes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Selected .cube file is empty"))
            }

            // 3. Compute SHA-256 Hash
            val hash = computeSha256(bytes)

            // 4. Parse and Validate LUT Structure
            val parseResult = CubeLutParser.parse(
                inputStream = ByteArrayInputStream(bytes),
                defaultTitle = displayName.removeSuffix(".cube").removeSuffix(".CUBE")
            )

            val definition = when (parseResult) {
                is LutValidationResult.Success -> parseResult.definition
                is LutValidationResult.Error -> {
                    return@withContext Result.failure(IllegalArgumentException("Failed to validate LUT: ${parseResult.message}"))
                }
            }

            // 5. Persist to App-Managed Internal Storage
            val targetFile = File(lutsDirectory, "${hash}.cube")
            if (!targetFile.exists()) {
                FileOutputStream(targetFile).use { it.write(bytes) }
            }

            Result.success(
                LutImportPayload(
                    originalFilename = displayName,
                    contentHash = hash,
                    fileSizeBytes = bytes.size.toLong(),
                    definition = definition,
                    storagePath = targetFile.absolutePath
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads and parses a stored .cube file from [storagePath].
     */
    suspend fun loadLutDefinition(storagePath: String): LutDefinition? = withContext(Dispatchers.IO) {
        val file = File(storagePath)
        if (!file.exists() || !file.canRead()) return@withContext null

        try {
            val result = CubeLutParser.parse(file.inputStream(), defaultTitle = file.nameWithoutExtension)
            if (result is LutValidationResult.Success) result.definition else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Deletes a stored LUT file from internal storage.
     */
    suspend fun deleteLutFile(storagePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(storagePath)
            if (file.exists() && file.parentFile?.absolutePath == lutsDirectory.absolutePath) {
                file.delete()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        fun computeSha256(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(bytes)
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }
}
