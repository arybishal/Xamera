package com.bishal.adaptivecamera.video.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.max

/**
 * Manages audio recording prerequisites, external microphone routing (FEAT-VID-05),
 * microphone permissions, and background real-time audio VU amplitude metering.
 */
class AudioRecordingManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _vuState = MutableStateFlow(AudioVuState.SILENCE)
    val vuState: StateFlow<AudioVuState> = _vuState.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _availableAudioInputs = MutableStateFlow<List<AudioInputDevice>>(emptyList())
    val availableAudioInputs: StateFlow<List<AudioInputDevice>> = _availableAudioInputs.asStateFlow()

    private val _selectedAudioInput = MutableStateFlow<AudioInputDevice?>(null)
    val selectedAudioInput: StateFlow<AudioInputDevice?> = _selectedAudioInput.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var meterJob: Job? = null
    private var isMonitoring = false
    private var deviceCallback: AudioDeviceCallback? = null

    init {
        initDeviceDiscovery()
    }

    private fun initDeviceDiscovery() {
        refreshAudioInputs()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    refreshAudioInputs()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    refreshAudioInputs()
                    val currentSelected = _selectedAudioInput.value
                    if (currentSelected != null && removedDevices?.any { it.id == currentSelected.id } == true) {
                        Log.w(TAG, "Selected audio device was disconnected. Falling back to default.")
                        selectAudioDevice(null)
                    }
                }
            }
            audioManager.registerAudioDeviceCallback(callback, null)
            deviceCallback = callback
        }
    }

    fun refreshAudioInputs() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = manager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            val currentSelectedId = _selectedAudioInput.value?.id
            val mapped = devices.map { device ->
                AudioInputDevice.fromDeviceInfo(device, isSelected = device.id == currentSelectedId)
            }
            _availableAudioInputs.value = mapped
        }
    }

    fun selectAudioDevice(device: AudioInputDevice?) {
        _selectedAudioInput.value = device
        refreshAudioInputs()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioRecord != null) {
            val manager = audioManager ?: return
            val devices = manager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            val targetDeviceInfo = if (device != null) {
                devices.firstOrNull { it.id == device.id }
            } else {
                null
            }
            val success = audioRecord?.setPreferredDevice(targetDeviceInfo)
            Log.d(TAG, "AudioRecord setPreferredDevice '${device?.name}': $success")
        }
    }

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Starts listening to audio amplitudes for the live VU meter.
     * Uses minimal buffer sizes and non-blocking background polling.
     */
    @SuppressLint("MissingPermission")
    fun startVuMonitoring() {
        if (isMonitoring || !hasRecordAudioPermission()) return

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (bufferSize <= 0) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.CAMCORDER,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            // Route to selected external audio device if configured
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && _selectedAudioInput.value != null) {
                val targetId = _selectedAudioInput.value?.id
                val dev = audioManager?.getDevices(AudioManager.GET_DEVICES_INPUTS)?.firstOrNull { it.id == targetId }
                if (dev != null) {
                    audioRecord?.setPreferredDevice(dev)
                }
            }

            audioRecord?.startRecording()
            isMonitoring = true

            meterJob = scope.launch(Dispatchers.Default) {
                val buffer = ShortArray(bufferSize / 2)
                var peakDecay = -60f

                while (isActive && isMonitoring) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var maxL = 0
                        var maxR = 0

                        for (i in 0 until read step 2) {
                            val sampleL = kotlin.math.abs(buffer[i].toInt())
                            if (sampleL > maxL) maxL = sampleL

                            if (i + 1 < read) {
                                val sampleR = kotlin.math.abs(buffer[i + 1].toInt())
                                if (sampleR > maxR) maxR = sampleR
                            } else {
                                maxR = maxL
                            }
                        }

                        val dbfsL = amplitudeToDbfs(maxL)
                        val dbfsR = amplitudeToDbfs(maxR)
                        val currentMax = max(dbfsL, dbfsR)

                        // Peak decay smoothing
                        peakDecay = max(currentMax, peakDecay - 1.5f)
                        val isClipping = currentMax >= -0.5f

                        _vuState.value = AudioVuState(
                            leftChannelDbfs = dbfsL,
                            rightChannelDbfs = dbfsR,
                            peakDbfs = peakDecay,
                            isClipping = isClipping,
                            isMuted = false
                        )
                    }
                    kotlinx.coroutines.delay(50) // ~20 updates/second for smooth UI
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio VU monitor initialization failed (graceful fallback)", e)
            stopVuMonitoring()
        }
    }

    /**
     * Stops the background audio meter and releases the microphone.
     */
    fun stopVuMonitoring() {
        isMonitoring = false
        meterJob?.cancel()
        meterJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audio recorder", e)
        }
        audioRecord = null
        _vuState.value = AudioVuState.SILENCE
    }

    /**
     * Cleans up device listeners and resources.
     */
    fun release() {
        stopVuMonitoring()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceCallback != null) {
            audioManager?.unregisterAudioDeviceCallback(deviceCallback)
            deviceCallback = null
        }
    }

    companion object {
        private const val TAG = "AudioRecordMgr"

        fun amplitudeToDbfs(amplitude: Int): Float {
            if (amplitude <= 0) return -60.0f
            val maxAmp = 32767.0
            val db = 20.0 * log10(amplitude.toDouble() / maxAmp)
            return db.toFloat().coerceIn(-60.0f, 0.0f)
        }
    }
}
