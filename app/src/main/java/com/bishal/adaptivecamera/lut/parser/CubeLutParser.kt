package com.bishal.adaptivecamera.lut.parser

import com.bishal.adaptivecamera.lut.model.ColorSpace
import com.bishal.adaptivecamera.lut.model.LutColor
import com.bishal.adaptivecamera.lut.model.LutDefinition
import com.bishal.adaptivecamera.lut.model.LutMetadata
import com.bishal.adaptivecamera.lut.model.LutType
import com.bishal.adaptivecamera.lut.model.LutValidationResult
import com.bishal.adaptivecamera.lut.model.TransferFunction
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader

/**
 * High-performance, robust parser for Adobe .cube format 1D and 3D Look-Up Tables.
 * Conforms to Adobe Cube LUT Specification v1.0.
 */
object CubeLutParser {

    const val MAX_SUPPORTED_3D_SIZE = 65 // Typically 17, 33, 64; 65^3 is ~823K floats (3.3MB)
    const val MAX_SUPPORTED_1D_SIZE = 4096

    fun parse(cubeText: String, defaultTitle: String = "Imported LUT"): LutValidationResult {
        return parse(StringReader(cubeText), defaultTitle)
    }

    fun parse(inputStream: InputStream, defaultTitle: String = "Imported LUT"): LutValidationResult {
        return parse(InputStreamReader(inputStream, Charsets.UTF_8), defaultTitle)
    }

    fun parse(reader: Reader, defaultTitle: String = "Imported LUT"): LutValidationResult {
        val bufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader)

        var title = defaultTitle
        var lutType: LutType? = null
        var size = -1
        var domainMin = LutColor(0.0f, 0.0f, 0.0f)
        var domainMax = LutColor(1.0f, 1.0f, 1.0f)
        val comments = mutableListOf<String>()

        var tableData: FloatArray? = null
        var floatsRead = 0
        var expectedFloats = 0
        var lineNumber = 0
        var hasDeclaredSize = false

        try {
            var line: String? = bufferedReader.readLine()
            while (line != null) {
                lineNumber++
                val trimmed = line.trim()

                // 1. Skip blank lines
                if (trimmed.isEmpty()) {
                    line = bufferedReader.readLine()
                    continue
                }

                // 2. Handle comment lines
                if (trimmed.startsWith("#")) {
                    val commentText = trimmed.removePrefix("#").trim()
                    if (commentText.isNotEmpty()) {
                        comments.add(commentText)
                    }
                    line = bufferedReader.readLine()
                    continue
                }

                // 3. Handle Header Keywords
                val upper = trimmed.uppercase()
                when {
                    upper.startsWith("TITLE") -> {
                        val rawTitle = trimmed.substring("TITLE".length).trim()
                        title = rawTitle.removeSurrounding("\"").ifBlank { defaultTitle }
                    }

                    upper.startsWith("LUT_3D_SIZE") -> {
                        if (hasDeclaredSize) {
                            return LutValidationResult.Error("Duplicate LUT size declaration on line $lineNumber", lineNumber)
                        }
                        val parts = trimmed.split("\\s+".toRegex())
                        if (parts.size < 2) {
                            return LutValidationResult.Error("Malformed LUT_3D_SIZE declaration on line $lineNumber", lineNumber)
                        }
                        val parsedSize = parts[1].toIntOrNull()
                            ?: return LutValidationResult.Error("Invalid integer for LUT_3D_SIZE: '${parts[1]}' on line $lineNumber", lineNumber)

                        if (parsedSize < 2) {
                            return LutValidationResult.Error("LUT_3D_SIZE ($parsedSize) must be at least 2", lineNumber)
                        }
                        if (parsedSize > MAX_SUPPORTED_3D_SIZE) {
                            return LutValidationResult.Error("LUT_3D_SIZE ($parsedSize) exceeds maximum safe limit ($MAX_SUPPORTED_3D_SIZE)", lineNumber)
                        }

                        lutType = LutType.LUT_3D
                        size = parsedSize
                        expectedFloats = size * size * size * 3
                        tableData = FloatArray(expectedFloats)
                        hasDeclaredSize = true
                    }

                    upper.startsWith("LUT_1D_SIZE") -> {
                        if (hasDeclaredSize) {
                            return LutValidationResult.Error("Duplicate LUT size declaration on line $lineNumber", lineNumber)
                        }
                        val parts = trimmed.split("\\s+".toRegex())
                        if (parts.size < 2) {
                            return LutValidationResult.Error("Malformed LUT_1D_SIZE declaration on line $lineNumber", lineNumber)
                        }
                        val parsedSize = parts[1].toIntOrNull()
                            ?: return LutValidationResult.Error("Invalid integer for LUT_1D_SIZE: '${parts[1]}' on line $lineNumber", lineNumber)

                        if (parsedSize < 2) {
                            return LutValidationResult.Error("LUT_1D_SIZE ($parsedSize) must be at least 2", lineNumber)
                        }
                        if (parsedSize > MAX_SUPPORTED_1D_SIZE) {
                            return LutValidationResult.Error("LUT_1D_SIZE ($parsedSize) exceeds maximum safe limit ($MAX_SUPPORTED_1D_SIZE)", lineNumber)
                        }

                        lutType = LutType.LUT_1D
                        size = parsedSize
                        expectedFloats = size * 3
                        tableData = FloatArray(expectedFloats)
                        hasDeclaredSize = true
                    }

                    upper.startsWith("DOMAIN_MIN") -> {
                        val parts = trimmed.split("\\s+".toRegex())
                        if (parts.size < 4) {
                            return LutValidationResult.Error("DOMAIN_MIN requires 3 float values on line $lineNumber", lineNumber)
                        }
                        val r = parts[1].toFloatOrNull()
                        val g = parts[2].toFloatOrNull()
                        val b = parts[3].toFloatOrNull()
                        if (r == null || g == null || b == null) {
                            return LutValidationResult.Error("Invalid numeric values for DOMAIN_MIN on line $lineNumber", lineNumber)
                        }
                        domainMin = LutColor(r, g, b)
                    }

                    upper.startsWith("DOMAIN_MAX") -> {
                        val parts = trimmed.split("\\s+".toRegex())
                        if (parts.size < 4) {
                            return LutValidationResult.Error("DOMAIN_MAX requires 3 float values on line $lineNumber", lineNumber)
                        }
                        val r = parts[1].toFloatOrNull()
                        val g = parts[2].toFloatOrNull()
                        val b = parts[3].toFloatOrNull()
                        if (r == null || g == null || b == null) {
                            return LutValidationResult.Error("Invalid numeric values for DOMAIN_MAX on line $lineNumber", lineNumber)
                        }
                        domainMax = LutColor(r, g, b)
                    }

                    else -> {
                        // 4. Data Line (RGB entry)
                        if (!hasDeclaredSize || tableData == null) {
                            return LutValidationResult.Error("Found LUT data entries before LUT size declaration on line $lineNumber", lineNumber)
                        }

                        val tokens = trimmed.split("\\s+".toRegex())
                        if (tokens.size < 3) {
                            return LutValidationResult.Error("Data entry requires at least 3 RGB values, found ${tokens.size} on line $lineNumber", lineNumber)
                        }

                        val r = tokens[0].toFloatOrNull()
                        val g = tokens[1].toFloatOrNull()
                        val b = tokens[2].toFloatOrNull()

                        if (r == null || g == null || b == null || !r.isFinite() || !g.isFinite() || !b.isFinite()) {
                            return LutValidationResult.Error("Invalid or non-finite floating point RGB numbers on line $lineNumber", lineNumber)
                        }

                        if (floatsRead + 3 > expectedFloats) {
                            return LutValidationResult.Error("Found more data entries than declared size (${expectedFloats / 3}) on line $lineNumber", lineNumber)
                        }

                        tableData[floatsRead++] = r
                        tableData[floatsRead++] = g
                        tableData[floatsRead++] = b
                    }
                }

                line = bufferedReader.readLine()
            }
        } catch (e: Exception) {
            return LutValidationResult.Error("I/O failure while reading .cube file: ${e.message}", lineNumber)
        }

        // 5. Final Structure Validations
        if (!hasDeclaredSize || lutType == null || tableData == null) {
            return LutValidationResult.Error("Invalid .cube file: Missing LUT_3D_SIZE or LUT_1D_SIZE header declaration")
        }

        if (domainMax.r <= domainMin.r || domainMax.g <= domainMin.g || domainMax.b <= domainMin.b) {
            return LutValidationResult.Error("Invalid DOMAIN boundaries: DOMAIN_MAX must be strictly greater than DOMAIN_MIN")
        }

        if (floatsRead < expectedFloats) {
            val entriesRead = floatsRead / 3
            val expectedEntries = expectedFloats / 3
            return LutValidationResult.Error("Truncated .cube file: Read $entriesRead entries, but expected $expectedEntries entries")
        }

        // Infer color space and transfer function from comments if tagged
        var inferredColorSpace = ColorSpace.REC_709
        var inferredTransfer = TransferFunction.REC_709
        for (c in comments) {
            val cu = c.uppercase()
            if (cu.contains("DCI-P3") || cu.contains("DISPLAY P3")) inferredColorSpace = ColorSpace.DCI_P3
            if (cu.contains("REC.2020") || cu.contains("BT.2020")) inferredColorSpace = ColorSpace.REC_2020
            if (cu.contains("HLG")) inferredTransfer = TransferFunction.HLG
            if (cu.contains("PQ") || cu.contains("ST2084") || cu.contains("ST 2084")) inferredTransfer = TransferFunction.PQ_ST2084
            if (cu.contains("S-LOG3") || cu.contains("SLOG3")) inferredTransfer = TransferFunction.S_LOG3
            if (cu.contains("LOG-C") || cu.contains("LOGC")) inferredTransfer = TransferFunction.LOG_C
        }

        val metadata = LutMetadata(
            title = title,
            type = lutType,
            size = size,
            domainMin = domainMin,
            domainMax = domainMax,
            inputColorSpace = inferredColorSpace,
            outputColorSpace = inferredColorSpace,
            transferFunction = inferredTransfer,
            comments = comments
        )

        return LutValidationResult.Success(LutDefinition(metadata, tableData))
    }
}
