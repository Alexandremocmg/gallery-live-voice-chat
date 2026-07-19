package com.google.ai.edge.gallery.voice.domain

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.os.SystemClock
import android.util.Log
import kotlin.math.abs
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class VoiceBargeInDetector {
  private val sessionLock = Any()
  @Volatile private var activeSession: DetectorSession? = null

  @SuppressLint("MissingPermission")
  fun start(
    scope: CoroutineScope,
    config: VoiceActivityDetectorConfig = BargeInSensitivityPolicy.BALANCED,
    onVoiceDetected: () -> Unit,
  ) {
    synchronized(sessionLock) {
      if (activeSession != null) return
    }
    val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    if (minBuffer <= 0) return
    val audioRecord = runCatching {
      AudioRecord(
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_FORMAT,
        minBuffer * 2,
      )
    }.getOrNull() ?: return
    if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
      audioRecord.release()
      return
    }

    val session = DetectorSession(recorder = audioRecord)
    synchronized(sessionLock) {
      if (activeSession != null) {
        audioRecord.release()
        return
      }
      activeSession = session
    }
    val job = scope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
      val echoCanceler =
        if (AcousticEchoCanceler.isAvailable()) {
          runCatching { AcousticEchoCanceler.create(audioRecord.audioSessionId) }.getOrNull()
        } else {
          null
        }
      runCatching { echoCanceler?.enabled = true }
      val detector = AdaptiveVoiceActivityDetector(config)
      detector.reset(SystemClock.elapsedRealtime())
      val samples = ShortArray(minBuffer / 2)
      try {
        audioRecord.startRecording()
        while (session.running) {
          val read = audioRecord.read(samples, 0, samples.size)
          if (read <= 0) continue
          var peak = 0
          for (index in 0 until read) peak = maxOf(peak, abs(samples[index].toInt()))
          if (detector.observe(peak, SystemClock.elapsedRealtime())) {
            session.voiceDetected = true
            session.running = false
          }
        }
      } catch (error: Exception) {
        if (session.running) Log.w(TAG, "Voice barge-in detector stopped", error)
      } finally {
        runCatching { audioRecord.stop() }
        runCatching { echoCanceler?.release() }
        runCatching { audioRecord.release() }
        session.running = false
      }
      val shouldNotify = synchronized(sessionLock) {
        if (activeSession === session) {
          activeSession = null
          session.voiceDetected
        } else {
          false
        }
      }
      if (shouldNotify) onVoiceDetected()
    }
    session.job = job
    job.start()
  }

  fun stop() {
    val session = synchronized(sessionLock) {
      activeSession?.also { activeSession = null }
    } ?: return
    session.running = false
    runCatching { session.recorder.stop() }
    runCatching { session.recorder.release() }
    session.job?.cancel()
  }

  fun release() {
    stop()
  }

  private data class DetectorSession(
    val recorder: AudioRecord,
    @Volatile var running: Boolean = true,
    @Volatile var voiceDetected: Boolean = false,
    @Volatile var job: Job? = null,
  )

  companion object {
    private const val TAG = "VoiceBargeIn"
    private const val SAMPLE_RATE = 16_000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
  }
}
