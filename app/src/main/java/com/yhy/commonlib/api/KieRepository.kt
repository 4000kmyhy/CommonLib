package com.yhy.commonlib.api

import android.util.Log
import com.google.gson.JsonSyntaxException
import com.yhy.commonlib.entity.RequestBuilder
import com.yhy.commonlib.entity.ValidateInfo
import com.yhy.commonlib.entity.VoiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * desc:
 **
 * user: xujj
 * time: 2026/6/11 17:21
 **/
class KieRepository constructor(
    private val service: KieService
) {

    companion object {
        private const val TAG = "KieRepository"

        @Volatile
        private var instance: KieRepository? = null

        fun getInstance(service: KieService = KieService.create()) =
            instance ?: synchronized(this) {
                instance ?: KieRepository(service).also { instance = it }
            }
    }

    suspend fun generateValidation(
        voiceUrl: String,
        vocalStartS: Int,
        vocalEndS: Int,
        language: String = "en"
    ) = withContext(Dispatchers.IO) {
        val request = RequestBuilder().apply {
            this["voiceUrl"] = voiceUrl
            this["vocalStartS"] = vocalStartS
            this["vocalEndS"] = vocalEndS
            this["language"] = language //en英文，zh中文，es西班牙语，fr法语，pt葡萄牙语言，de德语，ja日语，ko韩语，hi印地语，ru俄语
        }.build()
        val response = service.generateValidation(request = request)
        Log.d("xxx", "generateValidation: response=$response")
        response
    }

    suspend fun queryValidateInfo(taskId: String): ValidateInfo? = withContext(Dispatchers.IO) {
        while (true) {
            try {
                val response = service.getValidateInfo(taskId = taskId)
                Log.d("xxx", "queryValidateInfo: $response")
                if (response.code == 200) {
                    val info = response.data
                    if (info != null) {
                        when (info.status) {
                            "wait_processing ",
                            "processing_validate" -> { //仍在生成中
                                delay(5000)
                            }

                            else -> { //已生成或生成失败
                                return@withContext info
                            }
                        }
                    } else { //仍在生成中
                        delay(5000)
                    }
                } else {
                    return@withContext ValidateInfo(
                        errorCode = response.code,
                        errorMessage = response.msg
                    )
                }
            } catch (e: JsonSyntaxException) {
                break
            } catch (e: Exception) {
                Log.d("xxx", "queryValidateInfo: error=$e")
                delay(5000)
            }
        }
        return@withContext null
    }

    suspend fun regenerateValidation(taskId: String) = withContext(Dispatchers.IO) {
        val request = RequestBuilder().apply {
            this["taskId"] = taskId
        }.build()
        service.regenerateValidation(request = request)
    }

    suspend fun generateVoice(
        taskId: String,
        verifyUrl: String,
        voiceName: String,
        description: String,
        style: String,
        singerSkillLevel: String
    ) = withContext(Dispatchers.IO) {
        val request = RequestBuilder().apply {
            this["taskId"] = taskId
            this["verifyUrl"] = verifyUrl
            this["voiceName"] = voiceName
            this["description"] = description
            this["style"] = style
            this["singerSkillLevel"] = singerSkillLevel
        }.build()
        val response = service.generateVoice(request = request)
        Log.d("xxx", "generateVoice: response=$response")
        response
    }

    suspend fun queryVoiceInfo(taskId: String): VoiceInfo? = withContext(Dispatchers.IO) {
        while (true) {
            try {
                val response = service.getVoiceInfo(taskId = taskId)
                Log.d("xxx", "queryVoiceInfo: response=$response")
                if (response.code == 200) {
                    val info = response.data
                    if (info != null) {
                        when (info.status) {
                            "wait_processing ",
                            "processing_validate",
                            "wait_validating" -> { //仍在生成中
                                delay(5000)
                            }

                            else -> { //已生成或生成失败
                                return@withContext info
                            }
                        }
                    } else { //仍在生成中
                        delay(5000)
                    }
                } else {
                    return@withContext VoiceInfo(
                        errorCode = response.code,
                        errorMessage = response.msg
                    )
                }
            } catch (e: JsonSyntaxException) {
                break
            } catch (e: Exception) {
                Log.d("xxx", "queryVoiceInfo: error=$e")
                delay(5000)
            }
        }
        return@withContext null
    }

    suspend fun checkVoice(taskId: String): Boolean = withContext(Dispatchers.IO) {
        val request = RequestBuilder().apply {
            this["taskId"] = taskId
        }.build()
        val response = service.checkVoice(request = request)
        Log.d("xxx", "checkVoice: $response")
        response.data?.isAvailable ?: false
    }
}