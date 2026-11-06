package com.bishal.adaptivecamera.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

            CameraPreferences(
                gridType = gridType,
                isVirtualHorizonEnabled = isVirtualHorizonEnabled,
                aspectRatio = aspectRatio,
                isHapticsEnabled = isHapticsEnabled,
                isFloatingShutterEnabled = isFloatingShutterEnabled,
                floatingShutterX = floatingShutterX,
                floatingShutterY = floatingShutterY,
                isFrontScreenFlashEnabled = isFrontScreenFlashEnabled
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
}
