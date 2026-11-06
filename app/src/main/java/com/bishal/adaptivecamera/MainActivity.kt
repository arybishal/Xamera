package com.bishal.adaptivecamera

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bishal.adaptivecamera.core.display.DisplayRefreshRateManager
import com.bishal.adaptivecamera.core.storage.MediaItem
import com.bishal.adaptivecamera.gallery.ui.GalleryScreen
import com.bishal.adaptivecamera.gallery.ui.GalleryViewModel
import com.bishal.adaptivecamera.gallery.ui.MediaViewerScreen
import com.bishal.adaptivecamera.ui.camera.CameraScreen
import com.bishal.adaptivecamera.ui.camera.CameraViewModel
import com.bishal.adaptivecamera.ui.diagnostics.DiagnosticsScreen
import com.bishal.adaptivecamera.ui.diagnostics.DiagnosticsViewModel
import com.bishal.adaptivecamera.ui.lut.LutLibraryScreen
import com.bishal.adaptivecamera.ui.lut.LutViewModel
import com.bishal.adaptivecamera.ui.settings.SettingsScreen
import com.bishal.adaptivecamera.ui.theme.AdaptiveCameraTheme
import com.bishal.adaptivecamera.ui.theme.AppThemeMode
import com.bishal.adaptivecamera.ui.vault.RawVaultScreen

enum class AppDestination {
    CAMERA,
    GALLERY,
    MEDIA_VIEWER,
    SETTINGS,
    DIAGNOSTICS,
    RAW_VAULT,
    LUT_LIBRARY
}

class MainActivity : ComponentActivity() {

    private val cameraViewModel: CameraViewModel by viewModels()
    private val galleryViewModel: GalleryViewModel by viewModels()
    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels()
    private val lutViewModel: LutViewModel by viewModels()

    private var activeDestination = AppDestination.CAMERA

    private val bluetoothRemoteCoordinator by lazy {
        com.bishal.adaptivecamera.core.peripherals.BluetoothRemoteCoordinator(
            onTriggerShutter = {
                if (activeDestination == AppDestination.CAMERA) {
                    cameraViewModel.onShutterPressed()
                }
            },
            onZoomIn = {
                if (activeDestination == AppDestination.CAMERA) {
                    cameraViewModel.zoomIn()
                }
            },
            onZoomOut = {
                if (activeDestination == AppDestination.CAMERA) {
                    cameraViewModel.zoomOut()
                }
            }
        )
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (activeDestination == AppDestination.CAMERA) {
            if (bluetoothRemoteCoordinator.onKeyEvent(event.keyCode, event)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DisplayRefreshRateManager.applyOptimalRefreshRate(window)

        setContent {
            val cameraState by cameraViewModel.uiState.collectAsState()
            AdaptiveCameraTheme(themeMode = cameraState.themeMode) {
                MainAppNavHost(
                    activity = this,
                    cameraViewModel = cameraViewModel,
                    galleryViewModel = galleryViewModel,
                    diagnosticsViewModel = diagnosticsViewModel,
                    lutViewModel = lutViewModel,
                    onDestinationChanged = { activeDestination = it }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        DisplayRefreshRateManager.applyOptimalRefreshRate(window)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            DisplayRefreshRateManager.applyOptimalRefreshRate(window)
            val win = window ?: return
            val insetsController = WindowCompat.getInsetsController(win, win.decorView)
            if (activeDestination == AppDestination.CAMERA) {
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Ensure system status bar is restored when app moves to background
        window?.let {
            val insetsController = WindowCompat.getInsetsController(it, it.decorView)
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        }
    }
}

@Composable
fun MainAppNavHost(
    activity: ComponentActivity,
    cameraViewModel: CameraViewModel,
    galleryViewModel: GalleryViewModel,
    diagnosticsViewModel: DiagnosticsViewModel,
    lutViewModel: LutViewModel,
    onDestinationChanged: (AppDestination) -> Unit = {}
) {
    var currentDestination by remember { mutableStateOf(AppDestination.CAMERA) }
    var viewingItem by remember { mutableStateOf<MediaItem?>(null) }
    val cameraState by cameraViewModel.uiState.collectAsState()

    // Synchronize Immersive Status Bar with current destination
    LaunchedEffect(currentDestination, cameraState.themeMode) {
        onDestinationChanged(currentDestination)
        val window = activity.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isDark = when (cameraState.themeMode) {
            AppThemeMode.SYSTEM -> activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            AppThemeMode.DARK -> true
            AppThemeMode.LIGHT -> false
        }

        if (currentDestination == AppDestination.CAMERA) {
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
            insetsController.isAppearanceLightStatusBars = !isDark
        }
    }

    BackHandler(enabled = currentDestination != AppDestination.CAMERA) {
        when (currentDestination) {
            AppDestination.MEDIA_VIEWER -> {
                viewingItem = null
                currentDestination = AppDestination.GALLERY
            }
            AppDestination.RAW_VAULT, AppDestination.LUT_LIBRARY -> {
                currentDestination = AppDestination.SETTINGS
            }
            AppDestination.GALLERY, AppDestination.SETTINGS, AppDestination.DIAGNOSTICS -> {
                currentDestination = AppDestination.CAMERA
                cameraViewModel.refreshLatestThumbnail()
            }
            AppDestination.CAMERA -> Unit
        }
    }

    when (currentDestination) {
        AppDestination.CAMERA -> {
            CameraScreen(
                viewModel = cameraViewModel,
                onOpenGallery = {
                    galleryViewModel.loadMedia()
                    currentDestination = AppDestination.GALLERY
                },
                onOpenSettings = {
                    currentDestination = AppDestination.SETTINGS
                },
                onOpenLutLibrary = {
                    currentDestination = AppDestination.LUT_LIBRARY
                }
            )
        }
        AppDestination.GALLERY -> {
            GalleryScreen(
                viewModel = galleryViewModel,
                onNavigateBack = {
                    currentDestination = AppDestination.CAMERA
                    cameraViewModel.refreshLatestThumbnail()
                },
                onItemSelected = { item ->
                    viewingItem = item
                    currentDestination = AppDestination.MEDIA_VIEWER
                }
            )
        }
        AppDestination.MEDIA_VIEWER -> {
            viewingItem?.let { item ->
                MediaViewerScreen(
                    item = item,
                    onNavigateBack = {
                        viewingItem = null
                        currentDestination = AppDestination.GALLERY
                    },
                    onDeleteConfirmed = { toDelete ->
                        galleryViewModel.deleteItem(toDelete)
                        viewingItem = null
                        currentDestination = AppDestination.GALLERY
                        cameraViewModel.refreshLatestThumbnail()
                    }
                )
            } ?: run {
                currentDestination = AppDestination.GALLERY
            }
        }
        AppDestination.SETTINGS -> {
            SettingsScreen(
                diagnosticsViewModel = diagnosticsViewModel,
                cameraViewModel = cameraViewModel,
                onNavigateBack = {
                    currentDestination = AppDestination.CAMERA
                    cameraViewModel.refreshLatestThumbnail()
                },
                onNavigateToRawVault = {
                    currentDestination = AppDestination.RAW_VAULT
                },
                onNavigateToLutLibrary = {
                    currentDestination = AppDestination.LUT_LIBRARY
                }
            )
        }
        AppDestination.RAW_VAULT -> {
            RawVaultScreen(
                onNavigateBack = {
                    currentDestination = AppDestination.SETTINGS
                }
            )
        }
        AppDestination.LUT_LIBRARY -> {
            LutLibraryScreen(
                viewModel = lutViewModel,
                onNavigateBack = {
                    currentDestination = AppDestination.SETTINGS
                }
            )
        }
        AppDestination.DIAGNOSTICS -> {
            DiagnosticsScreen(
                viewModel = diagnosticsViewModel
            )
        }
    }
}
