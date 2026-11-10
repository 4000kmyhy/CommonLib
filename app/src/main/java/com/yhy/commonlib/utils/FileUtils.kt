package com.yhy.commonlib.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * desc:
 **
 * user: xujj
 * time: 2026/5/29 15:11
 **/
object FileUtils {

    fun parseFile(context: Context, assetName: String): String {
        return try {
            parseFile(context.assets.open(assetName))
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 解析文件流，返回字符串
     */
    private fun parseFile(inputStream: InputStream): String {
        var br: BufferedReader? = null
        val builder = StringBuilder()
        try {
            br = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                if (builder.isNotEmpty()) {
                    builder.append("\n")
                }
                builder.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                inputStream.close()
                br?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return builder.toString()
    }

    fun copyFile(
        srcFile: File,
        destFile: File
    ): Boolean {
        return try {
            destFile.parentFile?.mkdirs()
            destFile.delete()
            FileInputStream(srcFile).use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("xxx", "copyFile: "+e)
            false
        }
    }

    fun copyAssetFile(context: Context, assetName: String, outputFile: File) {
        try {
            //创建父目录，删除原文件
            outputFile.parentFile?.mkdirs()
            outputFile.delete()

            val buffer = ByteArray(1024)
            var length: Int
            context.assets.open(assetName).use { inputStream ->
                outputFile.outputStream().use { outputStream ->
                    while (inputStream.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                    outputStream.flush()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("xxx", "copyAssetFile: "+e)
        }
    }

    fun openDocumentIntent(
        mimeType: String = "audio/*",
        documentId: String = "primary:Music"
    ) = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeType

        // 可选：设置初始目录（API 26+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents",
                    documentId
                )
            )
        }
    }

    fun saveUri2File(context: Context, uri: Uri, file: File): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.delete()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}