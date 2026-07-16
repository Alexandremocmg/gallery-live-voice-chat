package com.google.ai.edge.gallery.voice.domain

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class VoiceAudioRecorder {
  private val sessionLock = Any()
  @Volatile private var activeSession: RecordingSession? = null

  val isRecording: Boolean
    get() = activeSession?.recording == true

  @SuppressLint("MissingPermission")
  fun start(
    scope: CoroutineScope,
    onComplete: (ByteArray) -> Unit,
    onError: (String) -> Unit,
  ) {
    synchronized(sessionLock) {
      if (activeSession != null) return
    }
    val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    if (minBufferSize <= 0) {
      onError("O gravador de audio nao esta disponivel neste celular.")
      return
    }

    val recorder =
      AudioRecord(
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_FORMAT,
        minBufferSize * 2,
      )
    if (recorder.state != AudioRecord.STATE_INITIALIZED) {
      recorder.release()
      onError("Nao foi possivel iniciar a gravacao de pronuncia.")
      return
    }

    val session = RecordingSession(recorder = recorder)
    synchronized(sessionLock) {
      if (activeSession != null) {
        recorder.release()
        return
      }
      activeSession = session
    }
    val job = scope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
      val pcm = ByteArrayOutputStream()
      val samples = ShortArray(minBufferSize / 2)
      val startedAt = SystemClock.elapsedRealtime()
      var speechStarted = false
      var lastSpeechAt = startedAt
      try {
        recorder.startRecording()
        while (session.recording && SystemClock.elapsedRealtime() - startedAt < MAX_DURATION_MS) {
          val read = recorder.read(samples, 0, samples.size)
          if (read <= 0) continue
          var peak = 0
          for (index in 0 until read) {
            val value = samples[index].toInt()
            peak = maxOf(peak, abs(value))
            pcm.write(value and 0xff)
            pcm.write(value shr 8 and 0xff)
          }
          val now = SystemClock.elapsedRealtime()
          if (peak >= SPEECH_PEAK_THRESHOLD) {
            speechStarted = true
            lastSpeechAt = now
          }
          if (speechStarted && now - lastSpeechAt >= END_SILENCE_MS) break
        }
      } catch (error: Exception) {
        if (session.recording) onError(error.message ?: "Falha ao gravar a tentativa.")
        session.submitWhenStopped = false
      } finally {
        session.recording = false
        runCatching { recorder.stop() }
        runCatching { recorder.release() }
      }

      val shouldSubmit = synchronized(sessionLock) {
        if (activeSession === session) {
          activeSession = null
          session.submitWhenStopped
        } else {
          false
        }
      }
      if (shouldSubmit) {
        val pcmBytes = pcm.toByteArray()
        if (speechStarted && pcmBytes.size >= MIN_AUDIO_BYTES) {
          onComplete(PcmWavEncoder.encodeMono16Bit(pcmBytes, SAMPLE_RATE))
        } else {
          onError("Nao consegui ouvir a tentativa. Tente falar um pouco mais perto do microfone.")
        }
      }
    }
    session.job = job
    job.start()
  }

  fun stop(submit: Boolean) {
    val session = synchronized(sessionLock) { activeSession } ?: return
    session.submitWhenStopped = submit
    session.recording = false
    runCatching { session.recorder.stop() }
  }

  fun release() {
    val session = synchronized(sessionLock) {
      activeSession?.also { activeSession = null }
    } ?: return
    session.submitWhenStopped = false
    session.recording = false
    runCatching { session.recorder.stop() }
    runCatching { session.recorder.release() }
    session.job?.cancel()
  }

  private data class RecordingSession(
    val recorder: AudioRecord,
    @Volatile var recording: Boolean = true,
    @Volatile var submitWhenStopped: Boolean = true,
    @Volatile var job: Job? = null,
  )
}

object PcmWavEncoder {
  fun encodeMono16Bit(pcm: ByteArray, sampleRate: Int): ByteArray {
    val header = ByteArray(44)
    val fileDataSize = pcm.size + 36
    writeAscii(header, 0, "RIFF")
    writeInt(header, 4, fileDataSize)
    writeAscii(header, 8, "WAVE")
    writeAscii(header, 12, "fmt ")
    writeInt(header, 16, 16)
    writeShort(header, 20, 1)
    writeShort(header, 22, 1)
    writeInt(header, 24, sampleRate)
    writeInt(header, 28, sampleRate * 2)
    writeShort(header, 32, 2)
    writeShort(header, 34, 16)
    writeAscii(header, 36, "data")
    writeInt(header, 40, pcm.size)
    return header + pcm
  }

  private fun writeAscii(target: ByteArray, offset: Int, value: String) {
    value.forEachIndexed { index, char -> target[offset + index] = char.code.toByte() }
  }

  private fun writeInt(target: ByteArray, offset: Int, value: Int) {
    repeat(4) { index -> target[offset + index] = (value shr (index * 8)).toByte() }
  }

  private fun writeShort(target: ByteArray, offset: Int, value: Int) {
    repeat(2) { index -> target[offset + index] = (value shr (index * 8)).toByte() }
  }
}

private const val SAMPLE_RATE = 16_000
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
private const val MAX_DURATION_MS = 12_000L
private const val END_SILENCE_MS = 1_100L
private const val SPEECH_PEAK_THRESHOLD = 850
private const val MIN_AUDIO_BYTES = SAMPLE_RATE / 2
