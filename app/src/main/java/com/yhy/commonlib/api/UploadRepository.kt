package com.yhy.commonlib.api

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.yhy.commonlib.entity.RequestBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * desc:
 **
 * user: xujj
 * time: 2026/4/21 16:45
 **/
class UploadRepository constructor(
    private val service: UploadService
) {

    companion object {
        private const val TAG = "UploadRepository"

        @Volatile
        private var instance: UploadRepository? = null

        fun getInstance(service: UploadService = UploadService.create()) =
            instance ?: synchronized(this) {
                instance ?: UploadRepository(service).also { instance = it }
            }

        const val uploadPath = "audios/user-uploads"

        suspend fun fileToUrl(file: File): String? {
            return try {
                getInstance().uploadFile(file)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun updateUrl(url: String): String = withContext(Dispatchers.IO) {
        val request = RequestBuilder().apply {
            this["fileUrl"] = url
            this["uploadPath"] = uploadPath
        }.build()

        val response = service.uploadUrl(request = request)

        if (!response.success)
            throw Exception("code=${response.code}, msg=${response.msg}")

        response.data.downloadUrl
    }

    suspend fun uploadFile(file: File): String = withContext(Dispatchers.IO) {
        val filePart = file.asRequestBody("application/octet-stream".toMediaType())
        val multipartFile = MultipartBody.Part.createFormData("file", file.name, filePart)
        val uploadPathPart = MultipartBody.Part.createFormData("uploadPath", uploadPath)

        val response = service.uploadFile(
            file = multipartFile,
            uploadPath = uploadPathPart
        )

        if (!response.success)
            throw Exception("code=${response.code}, msg=${response.msg}")

        response.data.downloadUrl
    }

    suspend fun uploadUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val body = uriToRequestBody(context, uri)

        if (body == null) {
            throw Exception("uri err=$uri")
        }

        val fileName = getFileName(context, uri) ?: "audio_file"
        val multipartFile = MultipartBody.Part.createFormData("file", fileName, body)
        val uploadPathPart = MultipartBody.Part.createFormData("uploadPath", uploadPath)

        val response = service.uploadFile(
            file = multipartFile,
            uploadPath = uploadPathPart
        )

        if (!response.success)
            throw Exception("code=${response.code}, msg=${response.msg}")

        response.data.downloadUrl
    }

    private fun uriToRequestBody(context: Context, uri: Uri): RequestBody? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()

            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null

        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameColumn = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameColumn >= 0) {
                        fileName = it.getString(nameColumn)
                    }
                }
            }
        }

        if (fileName == null) {
            fileName = uri.path?.substringAfterLast('/')
        }

        return fileName
    }
}