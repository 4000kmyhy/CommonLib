package com.yhy.commonlib.utils

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * desc:
 **
 * user: xujj
 * time: 2024/7/2 11:28
 **/
class PermissionUtils(private val activity: ComponentActivity) {

    companion object {
        val audioPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Manifest.permission.READ_EXTERNAL_STORAGE
            } else {
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }

        fun hasPermission(
            context: Context,
            permission: String = audioPermission
        ): Boolean {
            return hasPermission(context, arrayOf(permission))
        }

        fun hasPermission(
            context: Context,
            permissions: Array<String>
        ): Boolean {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
            return true
        }

        fun hasRecordPermission(context: Context): Boolean {
            return hasPermission(context, Manifest.permission.RECORD_AUDIO)
        }
    }

    private val launcherForPermissions: ActivityResultLauncher<Array<String>>
    private var shouldShowRequest = false
    private var permissionCallback: (Boolean) -> Unit = {}

    init {
        launcherForPermissions = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            var isGrant = false
            for (map in it) {
                if (map.value) {
                    isGrant = true
                } else {
                    isGrant = false
                    if (!shouldShowRequest &&
                        !shouldShowRequest(arrayOf(map.key))
                    ) {
                        goToSetting(map.key)
//                        showDialog(map.key)
                    }
                    break
                }
            }
            permissionCallback.invoke(isGrant)
        }.also {
            activity.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        it.unregister()
                    }
                }
            })
        }
    }

    fun launch(
        permission: String = audioPermission,
        callback: (Boolean) -> Unit = {}
    ) {
        launch(arrayOf(permission), callback)
    }

    fun launch(
        permissions: Array<String>,
        callback: (Boolean) -> Unit = {}
    ) {
        if (hasPermission(activity, permissions)) {
            callback(true)
        } else {
            permissionCallback = callback
            shouldShowRequest = shouldShowRequest(permissions)
            launcherForPermissions.launch(permissions)
        }
    }

    fun launchNotification(callback: (Boolean) -> Unit = {}) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = activity.getSystemService(NotificationManager::class.java)
            if (!manager.areNotificationsEnabled()) {
                launch(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    callback = callback
                )
            }
        }
    }

    private fun shouldShowRequest(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true
            }
        }
        return false
    }

    private fun showDialog(permission: String) {
//        val message = when (permission) {
//            Manifest.permission.RECORD_AUDIO -> {
//                activity.getString(R.string.permission_record)
//            }
//
//            Manifest.permission.POST_NOTIFICATIONS -> {
//                activity.getString(R.string.permission_notification)
//            }
//
//            audioPermission -> {
//                activity.getString(R.string.permission_audio)
//            }
//
//            else -> {
//                activity.getString(R.string.permission_def)
//            }
//        }
//        AlertDialog.Builder(activity)
//            .setCancelable(false)
//            .setTitle(activity.getString(R.string.settings))
//            .setMessage(message)
//            .setPositiveButton(activity.getString(R.string.to_setting)) { p0, p1 ->
//                goToSetting(permission)
//                p0?.dismiss()
//            }
//            .setNegativeButton(activity.getString(R.string.cancel)) { p0, p1 ->
//                p0?.dismiss()
//            }
//            .show()
    }

    private fun goToSetting(permission: String) {
        if (permission == Manifest.permission.POST_NOTIFICATIONS &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ) {
            try {
                val intent = Intent()
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.setData(Uri.parse("package:" + activity.packageName))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    val intent = Intent()
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.setData(Uri.parse("package:" + activity.packageName))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            try {
                val intent = Intent()
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.setData(Uri.parse("package:" + activity.packageName))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}