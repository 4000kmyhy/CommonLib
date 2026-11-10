package com.yhy.commonlib.entity

import com.google.gson.annotations.SerializedName

/**
 * desc:
 **
 * user: xujj
 * time: 2026/7/16 16:39
 **/
data class VoiceInfo(
    @SerializedName("taskId") val taskId: String? = "",
    @SerializedName("voiceId") val voiceId: String? = "",
    @SerializedName("status") val status: String? = "",
    @SerializedName("errorCode") val errorCode: Int = 0,
    @SerializedName("errorMessage") val errorMessage: String? = "",
)