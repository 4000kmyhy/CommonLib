package com.yhy.commonlib.entity

import com.google.gson.annotations.SerializedName

/**
 * desc:
 **
 * user: xujj
 * time: 2026/4/9 17:33
 **/
data class UploadResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("code")
    val code: Int,
    @SerializedName("msg")
    val msg: String,
    @SerializedName("data")
    val data: UploadData
) {
    data class UploadData(@SerializedName("downloadUrl") val downloadUrl: String)
}