@file:OptIn(UnstableApi::class)

package dev.shephard.player.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Volume normalization presets, shown as a switch-style choice (Spotify-like).
 * [targetDbfs] is the average (RMS) loudness the player aims for; null means no normalization.
 */
enum class VolumeNormalization(val value: String, val targetDbfs: Double?) {
    NONE("none", null),
    LOW("low", -20.0),
    NORMAL("normal", -16.0),
    HIGH("high", -12.0);

    companion object {
        fun fromValueOrDefault(value: String?): VolumeNormalization =
            entries.firstOrNull { it.value == value } ?: NONE
    }
}

/**
 * Process-wide state shared between the ViewModel (settings, track changes) and the audio
 * processor that runs inside the playback service. Both live in the same app process.
 */
object VolumeNormalizer {
    @Volatile
    var target: VolumeNormalization = VolumeNormalization.NONE

    private val resetRequested = AtomicBoolean(false)

    /** Restart loudness measurement, e.g. when a new track starts. */
    fun requestReset() {
        resetRequested.set(true)
    }

    internal fun consumeReset(): Boolean = resetRequested.getAndSet(false)
}

/**
 * Real-time loudness normalizer for PCM audio (16-bit and float).
 *
 * It measures the running RMS level of the audio that is actually being played, then moves the
 * gain toward `target - measured`. Limits and safety:
 *  - gain is limited to -12 dB .. +12 dB,
 *  - the gain never lets a block peak above ~98% full scale (prevents clipping),
 *  - gain drops quickly (avoids clipping on loud passages) and rises slowly (avoids pumping),
 *  - silence does not update the measurement, so pauses keep the previous gain.
 *
 * This works without reading file tags, so it applies to every track in the library.
 */
class VolumeNormalizationAudioProcessor : BaseAudioProcessor() {

    private var encoding: Int = C.ENCODING_INVALID
    private var smoothedPower: Double? = null
    private var gain: Float = 1f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        encoding = inputAudioFormat.encoding
        return if (encoding == C.ENCODING_PCM_16BIT || encoding == C.ENCODING_PCM_FLOAT) {
            inputAudioFormat
        } else {
            AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun onFlush() {
        // Seeks and track restarts: measure again from scratch, keep the current gain.
        smoothedPower = null
    }

    override fun onReset() {
        smoothedPower = null
        gain = 1f
        encoding = C.ENCODING_INVALID
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val start = inputBuffer.position()
        val end = inputBuffer.limit()
        val byteCount = end - start
        if (byteCount <= 0) return

        if (VolumeNormalizer.consumeReset()) smoothedPower = null

        val targetDbfs = VolumeNormalizer.target.targetDbfs
        val output = replaceOutputBuffer(byteCount)
        output.order(ByteOrder.nativeOrder())

        if (targetDbfs == null) {
            // Normalization off: pass the audio through unchanged.
            output.put(inputBuffer)
            output.flip()
            return
        }

        val bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val input = inputBuffer.duplicate().order(ByteOrder.nativeOrder())

        // Pass 1: measure the block.
        var sumSquares = 0.0
        var peak = 0f
        var count = 0
        var i = start
        while (i + bytesPerSample <= end) {
            val x = readSample(input, i)
            sumSquares += x * x
            val absX = abs(x)
            if (absX > peak) peak = absX
            count++
            i += bytesPerSample
        }
        if (count > 0) updateGain(sumSquares / count, peak, targetDbfs)

        // Pass 2: apply gain and write the output.
        val currentGain = gain
        i = start
        while (i + bytesPerSample <= end) {
            val x = readSample(input, i) * currentGain
            if (bytesPerSample == 4) {
                output.putFloat(x.coerceIn(-1f, 1f))
            } else {
                val scaled = (x * FULL_SCALE_16BIT).roundToInt().coerceIn(-32768, 32767)
                output.putShort(scaled.toShort())
            }
            i += bytesPerSample
        }

        inputBuffer.position(end)
        output.flip()
    }

    private fun readSample(buffer: ByteBuffer, index: Int): Float =
        if (encoding == C.ENCODING_PCM_FLOAT) {
            buffer.getFloat(index)
        } else {
            buffer.getShort(index) / FULL_SCALE_16BIT
        }

    private fun updateGain(blockMeanSquare: Double, blockPeak: Float, targetDbfs: Double) {
        if (blockMeanSquare < SILENCE_POWER) return

        val previous = smoothedPower
        val power = if (previous == null) {
            blockMeanSquare
        } else {
            previous + (blockMeanSquare - previous) * MEASURE_SMOOTHING
        }
        smoothedPower = power

        val measuredDbfs = 10.0 * log10(power)
        val wantedDb = (targetDbfs - measuredDbfs).coerceIn(-MAX_BOOST_DB, MAX_BOOST_DB)
        var desired = 10.0.pow(wantedDb / 20.0).toFloat()

        if (blockPeak > 0f) {
            desired = min(desired, PEAK_CEILING / blockPeak)
        }

        val coefficient = if (desired < gain) GAIN_DOWN_COEFFICIENT else GAIN_UP_COEFFICIENT
        gain += (desired - gain) * coefficient
    }

    private companion object {
        const val FULL_SCALE_16BIT = 32768f
        const val SILENCE_POWER = 1e-8          // about -80 dBFS RMS
        const val MEASURE_SMOOTHING = 0.02      // per block; roughly one second of audio
        const val MAX_BOOST_DB = 12.0
        const val PEAK_CEILING = 0.98f
        const val GAIN_DOWN_COEFFICIENT = 0.05f // fast reduction, avoids clipping
        const val GAIN_UP_COEFFICIENT = 0.01f   // slow increase, avoids pumping
    }
}
