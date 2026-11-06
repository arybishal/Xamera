package com.bishal.adaptivecamera.video.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
 * Manages audio recording prerequisites, microphone permissions,
 * and background real-time audio VU amplitude metering.
 */
class AudioRecordingManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _vuState = MutableStateFlow(AudioVuState.SILENCE)
    val vuState: StateFlow<AudioVuState> = _vuState.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var meterJob: Job? = null
    private var isMonitoring = false

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
