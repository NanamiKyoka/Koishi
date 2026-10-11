package com.nanami.koishi.feature.tools.noise_meter.engine

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.max

class NoiseMeterEngine(
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    private val blockSize: Int = DEFAULT_BLOCK_SIZE
) {

    @SuppressLint("MissingPermission")
    fun frames(config: NoiseConfig): Flow<NoiseFrame> = flow {
        val analyzer = WeightedPowerAnalyzer(config.frequencyWeighting, sampleRate, blockSize)
        val processor = NoiseProcessor(config.timeWeighting, blockSize * 1000.0 / sampleRate)
        val record = openRecord() ?: throw IllegalStateException("microphone unavailable")

        try {
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw IllegalStateException("unable to start recording")
            }

            val shortBuffer = ShortArray(blockSize)
            val samples = DoubleArray(blockSize)
            while (currentCoroutineContext().isActive) {
                val read = record.read(shortBuffer, 0, blockSize)
                if (read <= 0) {
                    if (read < 0) throw IllegalStateException("microphone read failed")
                    continue
                }
                for (i in 0 until read) samples[i] = shortBuffer[i] / 32768.0
                for (i in read until blockSize) samples[i] = 0.0
                emit(processor.process(analyzer.analyze(samples)))
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }.flowOn(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    private fun openRecord(): AudioRecord? {
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBuffer <= 0) return null
        val bufferSize = max(minBuffer, blockSize * 4)

        for (source in audioSources()) {
            val record = runCatching {
                AudioRecord(source, sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)
            }.getOrNull() ?: continue
            if (record.state == AudioRecord.STATE_INITIALIZED) return record
            record.release()
        }
        return null
    }

    private fun audioSources(): IntArray = intArrayOf(
        MediaRecorder.AudioSource.UNPROCESSED,
        MediaRecorder.AudioSource.MIC
    )

    companion object {
        const val DEFAULT_SAMPLE_RATE = 48000
        const val DEFAULT_BLOCK_SIZE = 2048
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
}