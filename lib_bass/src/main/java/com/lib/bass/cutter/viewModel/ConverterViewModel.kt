package com.lib.bass.cutter.viewModel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.lib.bass.utils.BassUtils
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS_FX
import com.un4seen.bass.BASSenc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * desc:
 **
 * user: xujj
 * time: 2024/4/21 15:14
 **/
class ConverterViewModel(application: Application) : BaseCutterViewModel(application) {

    var sourcePath: String? = null

    override fun onAudioFocusLoss() {

    }

    override fun saveFile(targetPath: String, format: String) {
        if (sourcePath == null) return

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            saveProgressModel.value = 0
            saveStateModel.value = false
            withContext(Dispatchers.IO) {
                val chan = BassUtils.streamCreateFile(sourcePath!!)
                val handle = BASS_FX.BASS_FX_TempoCreate(
                    chan,
                    BASS.BASS_SAMPLE_FLOAT or BASS.BASS_STREAM_DECODE
                )

                val encoder = BassUtils.getEncodeChannel(handle, targetPath, format)
                if (encoder == 0) {
                    BassUtils.logError("Encode")
                    return@withContext
                }

                val totalLength = BASS.BASS_ChannelGetLength(handle, BASS.BASS_POS_BYTE)
                var sum = 0
                var length = 0
                var progress = 0
                val buffer = ByteBuffer.allocate(1024)
                while (BASS.BASS_ChannelGetData(
                        handle,
                        buffer,
                        buffer.limit()
                    ).also { length = it } > 0
                ) {
                    sum += length
                    var progress2 = (sum * 1.0f / totalLength * 100).roundToInt()
                    progress2 = Math.min(progress2, 100)

                    if (progress != progress2) {
                        progress = progress2
                        withContext(Dispatchers.Main) {
                            saveProgressModel.value = progress
                        }
                    }
                }

                //释放、结束编码
                BASS.BASS_StreamFree(handle)
                BASSenc.BASS_Encode_Stop(encoder)

                withContext(Dispatchers.Main) {
                    saveStateModel.value = true
                }
            }
        }
    }
}