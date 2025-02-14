package com.lib.automix.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
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
    }

    private var launcherForPermissions: ActivityResultLauncher<Array<String>>? = null
    private var shouldShowRequest = false
    private var permissionCallback: ((Boolean) -> Unit)? = null

    fun create() {
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
                        goToSetting()
                    }
                    break
                }
            }
            permissionCallback?.invoke(isGrant)
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
        permissionCallback = callback
        shouldShowRequest = shouldShowRequest(permissions)
        launcherForPermissions?.launch(permissions)
    }

    private fun shouldShowRequest(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true
            }
        }
        return false
    }

    private fun goToSetting() {
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