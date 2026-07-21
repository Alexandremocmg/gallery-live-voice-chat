package com.google.ai.edge.gallery.voice.domain

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import java.io.IOException
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class InjectedAudioSource {
  @SuppressLint("MissingPermission")
  fun start(
    scope: CoroutineScope,
    onError: (String) -> Unit,
  ): InjectedAudioSession? {
    val minBufferSize =
      AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_ENCODING)
    if (minBufferSize <= 0) {
      onError("O audio controlado nao esta disponivel neste aparelho.")
      return null
    }

    val recorder =
      AudioRecord(
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_ENCODING,
        minBufferSize * 2,
      )
    if (recorder.state != AudioRecord.STATE_INITIALIZED) {
      recorder.release()
      onError("Nao foi possivel abrir o microfone controlado.")
      return null
    }

    val pipe = runCatching { ParcelFileDescriptor.createPipe() }.getOrElse { error ->
      recorder.release()
      onError(error.message ?: "Nao foi possivel criar o canal de audio.")
      return null
    }
    val session =
      InjectedAudioSession(
        readDescriptor = pipe[0],
        writeDescriptor = pipe[1],
        recorder = recorder,
      )
    session.job = scope.launch(Dispatchers.IO) {
      val buffer = ByteArray(maxOf(minBufferSize, 3_200))
      try {
        ParcelFileDescriptor.AutoCloseOutputStream(session.writeDescriptor).use { output ->
          recorder.startRecording()
          val startedAt = SystemClock.elapsedRealtime()
          var speechStarted = false
          var lastSpeechAt = startedAt
          while (session.active) {
            val read = recorder.read(buffer, 0, buffer.size)
            when {
              read > 0 -> {
                output.write(buffer, 0, read)
                val peak = pcm16Peak(buffer, read)
                val now = SystemClock.elapsedRealtime()
                if (peak >= SPEECH_PEAK_THRESHOLD) {
                  speechStarted = true
                  lastSpeechAt = now
                }
                if ((!speechStarted && now - startedAt >= INITIAL_SPEECH_WINDOW_MS) ||
                  (speechStarted && now - lastSpeechAt >= END_SILENCE_MS) ||
                  now - startedAt >= MAX_DURATION_MS
                ) {
                  break
                }
              }
              read == AudioRecord.ERROR_DEAD_OBJECT -> throw IOException("AudioRecord foi encerrado")
              read < 0 && session.active -> throw IOException("Falha de captura: $read")
            }
          }
          output.flush()
        }
      } catch (error: Exception) {
        if (session.active) onError(error.message ?: "Falha no audio controlado.")
      } finally {
        session.active = false
        runCatching { recorder.stop() }
        recorder.release()
        runCatching { session.writeDescriptor.close() }
      }
    }
    return session
  }

  internal fun pcm16Peak(buffer: ByteArray, size: Int): Int {
    var peak = 0
    var index = 0
    val limit = size - (size % 2)
    while (index < limit) {
      val low = buffer[index].toInt() and 0xff
      val high = buffer[index + 1].toInt()
      val sample = ((high shl 8) or low).toShort().toInt()
      peak = maxOf(peak, abs(sample))
      index += 2
    }
    return peak
  }

  companion object {
    const val SAMPLE_RATE = 16_000
    const val CHANNEL_COUNT = 1
    const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    const val INITIAL_SPEECH_WINDOW_MS = 6_000L
    const val END_SILENCE_MS = 1_100L
    const val MAX_DURATION_MS = 20_000L
    private const val SPEECH_PEAK_THRESHOLD = 850
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
  }
}

class InjectedAudioSession internal constructor(
  val readDescriptor: ParcelFileDescriptor,
  internal val writeDescriptor: ParcelFileDescriptor,
  internal val recorder: AudioRecord,
) {
  @Volatile internal var active: Boolean = true
  @Volatile internal var job: Job? = null

  fun stop() {
    active = false
    runCatching { recorder.stop() }
    runCatching { writeDescriptor.close() }
  }

  fun close() {
    stop()
    runCatching { readDescriptor.close() }
    job?.cancel()
  }
}
