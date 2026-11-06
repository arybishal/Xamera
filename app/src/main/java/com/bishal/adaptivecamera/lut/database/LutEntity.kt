package com.bishal.adaptivecamera.lut.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bishal.adaptivecamera.lut.model.ColorSpace
import com.bishal.adaptivecamera.lut.model.LutColor
import com.bishal.adaptivecamera.lut.model.LutMetadata
import com.bishal.adaptivecamera.lut.model.LutSourceType
import com.bishal.adaptivecamera.lut.model.LutType
import com.bishal.adaptivecamera.lut.model.TransferFunction

@Entity(
    tableName = "luts",
    indices = [
        Index(value = ["contentHash"], unique = false),
        Index(value = ["sourceType"]),
        Index(value = ["isFavorite"])
    ]
)
data class LutEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val sourceType: String, // BUILT_IN or IMPORTED
    val originalFileName: String? = null,
    val lutType: String, // LUT_1D or LUT_3D
    val dimension: Int,
    val domainMinR: Float = 0f,
    val domainMinG: Float = 0f,
    val domainMinB: Float = 0f,
    val domainMaxR: Float = 1f,
    val domainMaxG: Float = 1f,
    val domainMaxB: Float = 1f,
    val contentHash: String,
    val storagePath: String? = null,
    val isFavorite: Boolean = false,
    val dateAddedMs: Long,
    val fileSizeBytes: Long = 0L,
    val inputColorSpace: String = ColorSpace.REC_709.id,
    val outputColorSpace: String = ColorSpace.REC_709.id,
    val transferFunction: String = TransferFunction.REC_709.id
) {
    fun toLutMetadata(): LutMetadata {
        return LutMetadata(
            title = name,
            type = if (lutType == LutType.LUT_1D.name) LutType.LUT_1D else LutType.LUT_3D,
            size = dimension,
            domainMin = LutColor(domainMinR, domainMinG, domainMinB),
            domainMax = LutColor(domainMaxR, domainMaxG, domainMaxB),
            inputColorSpace = ColorSpace.fromId(inputColorSpace),
            outputColorSpace = ColorSpace.fromId(outputColorSpace),
            transferFunction = TransferFunction.fromId(transferFunction)
        )
    }

    val isBuiltIn: Boolean
        get() = sourceType == LutSourceType.BUILT_IN.name
}
