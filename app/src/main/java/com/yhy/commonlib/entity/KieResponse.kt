package com.yhy.commonlib.entity

import com.google.gson.annotations.SerializedName

/**
 * desc:
 **
 * user: xujj
 * time: 2026/7/16 10:59
 **/
data class KieResponse<T>(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("msg")
    val msg: String = "",
    @SerializedName("data")
    val data: T? = null
)

data class KieTask(
    @SerializedName("taskId") val taskId: String
)

data class VoiceAvailable(
    @SerializedName("isAvailable") val isAvailable: Boolean = false
)