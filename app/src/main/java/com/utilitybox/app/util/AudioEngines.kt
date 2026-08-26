package com.utilitybox.app.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private const val SAMPLE_RATE = 44_100

enum class ToneChannel { BOTH, LEFT, RIGHT }

private fun buildTrack(bufferBytes: Int): AudioTrack =
    AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
        )
        .setBufferSizeInBytes(bufferBytes)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

private fun minStereoBuffer(): Int = maxOf(
    AudioTrack.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT,
    ),
    4096,
)

/**
 * Continuous sine wave with click-free frequency changes: phase is carried across
 * buffers so sweeping the frequency slider does not produce pops.
 */
class TonePlayer {
    private val running = AtomicBoolean(false)
    private val frequency = AtomicLong(java.lang.Double.doubleToRawLongBits(440.0))
    private val amplitude = AtomicLong(java.lang.Double.doubleToRawLongBits(0.5))
    private val channel = AtomicInteger(ToneChannel.BOTH.ordinal)

    @Volatile
    private var track: AudioTrack? = null

    @Volatile
    private var thread: Thread? = null

    fun setFrequency(hz: Double) {
        frequency.set(java.lang.Double.doubleToRawLongBits(hz.coerceIn(10.0, 22_000.0)))
    }

    fun setVolume(value: Float) {
        amplitude.set(java.lang.Double.doubleToRawLongBits(value.coerceIn(0f, 1f).toDouble()))
    }

    fun setChannel(value: ToneChannel) = channel.set(value.ordinal)

    fun start() {
        if (running.getAndSet(true)) return
        val bufferBytes = minStereoBuffer()
        val audioTrack = runCatching { buildTrack(bufferBytes) }.getOrNull() ?: run {
            running.set(false)
            return
        }
        track = audioTrack
        audioTrack.play()

        thread = Thread {
            val frames = bufferBytes / 4
            val buffer = ShortArray(frames * 2)
            var phase = 0.0
            while (running.get()) {
                val hz = java.lang.Double.longBitsToDouble(frequency.get())
                val amp = java.lang.Double.longBitsToDouble(amplitude.get())
                val mode = ToneChannel.entries[channel.get()]
                val step = 2.0 * PI * hz / SAMPLE_RATE
                for (i in 0 until frames) {
                    val sample = (sin(phase) * amp * Short.MAX_VALUE).toInt().toShort()
                    buffer[i * 2] = if (mode == ToneChannel.RIGHT) 0 else sample
                    buffer[i * 2 + 1] = if (mode == ToneChannel.LEFT) 0 else sample
                    phase += step
                    if (phase > 2 * PI) phase -= 2 * PI
                }
                val written = try {
                    audioTrack.write(buffer, 0, buffer.size)
                } catch (error: IllegalStateException) {
                    -1
                }
                if (written < 0) break
            }
        }.also { it.isDaemon = true; it.start() }
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        thread?.join(500)
        thread = null
        runCatching {
            track?.pause()
            track?.flush()
            track?.stop()
            track?.release()
        }
        track = null
    }
}

/**
 * Sample-accurate metronome. Clicks are placed by frame index rather than by
 * sleeping a thread, so the tempo does not drift over long practice sessions.
 */
class MetronomeEngine {
    private val running = AtomicBoolean(false)
    private val bpm = AtomicInteger(120)
    private val beatsPerBar = AtomicInteger(4)
    private val beatCounter = AtomicInteger(0)

    @Volatile
    private var track: AudioTrack? = null

    @Volatile
    private var thread: Thread? = null

    /** Increments once per click so the UI can flash in time. */
    val beat: Int get() = beatCounter.get()

    fun setTempo(value: Int) = bpm.set(value.coerceIn(30, 260))

    fun setBeatsPerBar(value: Int) = beatsPerBar.set(value.coerceIn(1, 12))

    fun start() {
        if (running.getAndSet(true)) return
        val bufferBytes = minStereoBuffer()
        val audioTrack = runCatching { buildTrack(bufferBytes) }.getOrNull() ?: run {
            running.set(false)
            return
        }
        track = audioTrack
        beatCounter.set(0)
        audioTrack.play()

        thread = Thread {
            val frames = bufferBytes / 4
            val buffer = ShortArray(frames * 2)
            var framesUntilClick = 0L
            var clickIndex = 0
            var beatInBar = 0

            while (running.get()) {
                val framesPerBeat = (SAMPLE_RATE * 60.0 / bpm.get()).toLong()
                java.util.Arrays.fill(buffer, 0)

                for (i in 0 until frames) {
                    if (framesUntilClick <= 0L) {
                        framesUntilClick = framesPerBeat
                        clickIndex = 0
                        beatInBar = if (beatInBar + 1 > beatsPerBar.get()) 1 else beatInBar + 1
                        beatCounter.incrementAndGet()
                    }
                    if (clickIndex < CLICK_FRAMES) {
                        val accented = beatInBar == 1
                        val sample = clickSample(clickIndex, accented)
                        buffer[i * 2] = sample
                        buffer[i * 2 + 1] = sample
                        clickIndex++
                    }
                    framesUntilClick--
                }
                val written = try {
                    audioTrack.write(buffer, 0, buffer.size)
                } catch (error: IllegalStateException) {
                    -1
                }
                if (written < 0) break
            }
        }.also { it.isDaemon = true; it.start() }
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        thread?.join(500)
        thread = null
        runCatching {
            track?.pause()
            track?.flush()
            track?.stop()
            track?.release()
        }
        track = null
    }

    private companion object {
        const val CLICK_FRAMES = 1200

        /** A short decaying sine burst: higher pitch marks the downbeat. */
        fun clickSample(index: Int, accented: Boolean): Short {
            val frequency = if (accented) 1600.0 else 1000.0
            val decay = exp(-index / 180.0)
            val value = sin(2.0 * PI * frequency * index / SAMPLE_RATE) * decay * 0.6
            return (value * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
