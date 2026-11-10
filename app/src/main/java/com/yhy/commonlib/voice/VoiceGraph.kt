package com.yhy.commonlib.voice

import kotlinx.serialization.Serializable

/**
 * desc:
 **
 * user: xujj
 * time: 2026/7/17 14:43
 **/
sealed class VoiceGraph {

    @Serializable
    data object VoiceValidate

    @Serializable
    data class VoiceGenerate(val validateJson: String)
}