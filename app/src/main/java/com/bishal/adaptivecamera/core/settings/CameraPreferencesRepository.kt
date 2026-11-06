package com.bishal.adaptivecamera.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bishal.adaptivecamera.camera.config.AspectRatioConfig
import com.bishal.adaptivecamera.camera.config.CompositionGridType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "xamera_preferences")

interface CameraPreferencesRepository {
    val preferencesFlow: Flow<CameraPreferences>

    suspend fun updateGridType(gridType: CompositionGridType)
    suspend fun updateVirtualHorizonEnabled(enabled: Boolean)
    suspend fun updateAspectRatio(aspectRatio: AspectRatioConfig)
    suspend fun updateHapticsEnabled(enabled: Boolean)
    suspend fun updateFloatingShutterEnabled(enabled: Boolean)
    suspend fun updateFloatingShutterPosition(x: Float, y: Float)
    suspend fun resetFloatingShutterPosition()
    suspend fun updateFrontScreenFlashEnabled(enabled: Boolean)
    suspend fun updateZslEnabled(enabled: Boolean)
    suspend fun updateAebBracketCount(count: Int)
    suspend fun updateAebEvStep(step: Float)
    suspend fun updateCaptureFormat(format: String)
    suspend fun updateWatermarkEnabled(enabled: Boolean)
    suspend fun updateWatermarkType(type: String)
    suspend fun updateWatermarkText(text: String)
    suspend fun updateExifPrivacyEnabled(enabled: Boolean)
    suspend fun updateStripGpsLocation(enabled: Boolean)
    suspend fun updateStripDeviceModel(enabled: Boolean)
}

class CameraPreferencesRepositoryImpl(
    private val context: Context
) : CameraPreferencesRepository {

    private object PreferencesKeys {
        val KEY_GRID_TYPE = stringPreferencesKey("grid_type")
        val KEY_VIRTUAL_HORIZON_ENABLED = booleanPreferencesKey("virtual_horizon_enabled")
        val KEY_ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
        val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val KEY_FLOATING_SHUTTER_ENABLED = booleanPreferencesKey("floating_shutter_enabled")
        val KEY_FLOATING_SHUTTER_X = floatPreferencesKey("floating_shutter_x")
        val KEY_FLOATING_SHUTTER_Y = floatPreferencesKey("floating_shutter_y")
        val KEY_FRONT_SCREEN_FLASH_ENABLED = booleanPreferencesKey("front_screen_flash_enabled")
        val KEY_ZSL_ENABLED = booleanPreferencesKey("zsl_enabled")
        val KEY_AEB_BRACKET_COUNT = intPreferencesKey("aeb_bracket_count")
        val KEY_AEB_EV_STEP = floatPreferencesKey("aeb_ev_step")
        val KEY_CAPTURE_FORMAT = stringPreferencesKey("capture_format")
        val KEY_WATERMARK_ENABLED = booleanPreferencesKey("watermark_enabled")
        val KEY_WATERMARK_TYPE = stringPreferencesKey("watermark_type")
        val KEY_WATERMARK_TEXT = stringPreferencesKey("watermark_text")
        val KEY_EXIF_PRIVACY_ENABLED = booleanPreferencesKey("exif_privacy_enabled")
        val KEY_STRIP_GPS_LOCATION = booleanPreferencesKey("strip_gps_location")
        val KEY_STRIP_DEVICE_MODEL = booleanPreferencesKey("strip_device_model")
    }

    override val preferencesFlow: Flow<CameraPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val gridTypeName = preferences[PreferencesKeys.KEY_GRID_TYPE] ?: CompositionGridType.OFF.name
            val gridType = try {
                CompositionGridType.valueOf(gridTypeName)
            } catch (_: Exception) {
                CompositionGridType.OFF
            }

            val isVirtualHorizonEnabled = preferences[PreferencesKeys.KEY_VIRTUAL_HORIZON_ENABLED] ?: true

            val aspectRatioName = preferences[PreferencesKeys.KEY_ASPECT_RATIO] ?: AspectRatioConfig.RATIO_4_3.name
            val aspectRatio = try {
                AspectRatioConfig.valueOf(aspectRatioName)
            } catch (_: Exception) {
                AspectRatioConfig.RATIO_4_3
            }

            val isHapticsEnabled = preferences[PreferencesKeys.KEY_HAPTICS_ENABLED] ?: true
            val isFloatingShutterEnabled = preferences[PreferencesKeys.KEY_FLOATING_SHUTTER_ENABLED] ?: false
            val floatingShutterX = preferences[PreferencesKeys.KEY_FLOATING_SHUTTER_X] ?: 0.82f
            val floatingShutterY = preferences[PreferencesKeys.KEY_FLOATING_SHUTTER_Y] ?: 0.65f
            val isFrontScreenFlashEnabled = preferences[PreferencesKeys.KEY_FRONT_SCREEN_FLASH_ENABLED] ?: true
            val isZslEnabled = preferences[PreferencesKeys.KEY_ZSL_ENABLED] ?: true
            val aebBracketCount = preferences[PreferencesKeys.KEY_AEB_BRACKET_COUNT] ?: 3
            val aebEvStep = preferences[PreferencesKeys.KEY_AEB_EV_STEP] ?: 1.0f
            val captureFormat = preferences[PreferencesKeys.KEY_CAPTURE_FORMAT] ?: "JPEG"
            val isWatermarkEnabled = preferences[PreferencesKeys.KEY_WATERMARK_ENABLED] ?: false
            val watermarkType = preferences[PreferencesKeys.KEY_WATERMARK_TYPE] ?: "TEXT"
            val watermarkText = preferences[PreferencesKeys.KEY_WATERMARK_TEXT] ?: "Xamera Professional"
            val isExifPrivacyEnabled = preferences[PreferencesKeys.KEY_EXIF_PRIVACY_ENABLED] ?: false
            val stripGpsLocation = preferences[PreferencesKeys.KEY_STRIP_GPS_LOCATION] ?: true
            val stripDeviceModel = preferences[PreferencesKeys.KEY_STRIP_DEVICE_MODEL] ?: true

            CameraPreferences(
                gridType = gridType,
                isVirtualHorizonEnabled = isVirtualHorizonEnabled,
                aspectRatio = aspectRatio,
                isHapticsEnabled = isHapticsEnabled,
                isFloatingShutterEnabled = isFloatingShutterEnabled,
                floatingShutterX = floatingShutterX,
                floatingShutterY = floatingShutterY,
                isFrontScreenFlashEnabled = isFrontScreenFlashEnabled,
                isZslEnabled = isZslEnabled,
                aebBracketCount = aebBracketCount,
                aebEvStep = aebEvStep,
                captureFormat = captureFormat,
                isWatermarkEnabled = isWatermarkEnabled,
                watermarkType = watermarkType,
                watermarkText = watermarkText,
                isExifPrivacyEnabled = isExifPrivacyEnabled,
                stripGpsLocation = stripGpsLocation,
                stripDeviceModel = stripDeviceModel
            )
        }

    override suspend fun updateGridType(gridType: CompositionGridType) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_GRID_TYPE] = gridType.name
        }
    }

    override suspend fun updateVirtualHorizonEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_VIRTUAL_HORIZON_ENABLED] = enabled
        }
    }

    override suspend fun updateAspectRatio(aspectRatio: AspectRatioConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_ASPECT_RATIO] = aspectRatio.name
        }
    }

    override suspend fun updateHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_HAPTICS_ENABLED] = enabled
        }
    }

    override suspend fun updateFloatingShutterEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_FLOATING_SHUTTER_ENABLED] = enabled
        }
    }

    override suspend fun updateFloatingShutterPosition(x: Float, y: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_FLOATING_SHUTTER_X] = x
            preferences[PreferencesKeys.KEY_FLOATING_SHUTTER_Y] = y
        }
    }

    override suspend fun resetFloatingShutterPosition() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_FLOATING_SHUTTER_X] = 0.82f
            preferences[PreferencesKeys.KEY_FLOATING_SHUTTER_Y] = 0.65f
        }
    }

    override suspend fun updateFrontScreenFlashEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_FRONT_SCREEN_FLASH_ENABLED] = enabled
        }
    }

    override suspend fun updateZslEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_ZSL_ENABLED] = enabled
        }
    }

    override suspend fun updateAebBracketCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_AEB_BRACKET_COUNT] = count
        }
    }

    override suspend fun updateAebEvStep(step: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_AEB_EV_STEP] = step
        }
    }

    override suspend fun updateCaptureFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_CAPTURE_FORMAT] = format
        }
    }

    override suspend fun updateWatermarkEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_WATERMARK_ENABLED] = enabled
        }
    }

    override suspend fun updateWatermarkType(type: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_WATERMARK_TYPE] = type
        }
    }

    override suspend fun updateWatermarkText(text: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_WATERMARK_TEXT] = text
        }
    }

    override suspend fun updateExifPrivacyEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_EXIF_PRIVACY_ENABLED] = enabled
        }
    }

    override suspend fun updateStripGpsLocation(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_STRIP_GPS_LOCATION] = enabled
        }
    }

    override suspend fun updateStripDeviceModel(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_STRIP_DEVICE_MODEL] = enabled
        }
    }
}
