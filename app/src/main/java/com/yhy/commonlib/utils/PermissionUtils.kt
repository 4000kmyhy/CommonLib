package com.yhy.commonlib.utils

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.yhy.commonlib.utils.PermissionUtils.Companion.goToSetting

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

        val writePermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        } else {
            ""
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

        fun showDialog(context: Context, permission: String) {
//            val message = when (permission) {
//                Manifest.permission.RECORD_AUDIO -> context.getString(R.string.permission_record)
//                Manifest.permission.POST_NOTIFICATIONS -> context.getString(R.string.permission_notification)
//                audioPermission -> context.getString(R.string.permission_audio)
//                else -> context.getString(R.string.permission_def)
//            }
//            AlertDialog.Builder(context)
//                .setCancelable(false)
//                .setTitle(R.string.settings)
//                .setMessage(message)
//                .setPositiveButton(R.string.to_setting) { p0, p1 ->
//                    goToSetting(context, permission)
//                    p0?.dismiss()
//                }
//                .setNegativeButton(R.string.cancel) { p0, p1 ->
//                    p0?.dismiss()
//                }
//                .show()
        }

        fun goToSetting(context: Context, permission: String) {
            val intent = Intent().apply {
                setData("package:${context.packageName}".toUri())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (permission == Manifest.permission.POST_NOTIFICATIONS &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ) {
                try {
                    context.startActivity(intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS))
                } catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        context.startActivity(intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                try {
                    context.startActivity(intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
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
                        goToSetting(activity, map.key)
//                        showDialog(activity, map.key)
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
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun requestPermission(
    permission: String,
    callback: () -> Unit
): () -> Unit {
    if (permission.isEmpty()) return callback

    val activity = (LocalContext.current) as Activity
    var shouldShowRationale by remember { mutableStateOf(false) }
    val permissionState = rememberPermissionState(
        permission = permission,
        onPermissionResult = {
            if (it) {
                callback()
            } else {
                if (!shouldShowRationale &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                ) {
//                    PermissionUtils.showDialog(activity, permission)
                    goToSetting(activity, permission)
                }
            }
        }
    )
    return {
        if (permissionState.status.isGranted) {
            callback()
        } else {
            shouldShowRationale = permissionState.status.shouldShowRationale
            permissionState.launchPermissionRequest()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun requestPermissions(
    permissions: List<String>,
    callback: () -> Unit
): () -> Unit {
    if (permissions.isEmpty()) return callback

    val activity = (LocalContext.current) as Activity
    var shouldShowRationale by remember { mutableStateOf(false) }
    val permissionState = rememberMultiplePermissionsState(
        permissions = permissions,
        onPermissionsResult = {
            var isAllGrant = false
            for (map in it) {
                if (map.value) {
                    isAllGrant = true
                } else {
                    isAllGrant = false
                    if (!shouldShowRationale &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(activity, map.key)
                    ) {
                        goToSetting(activity, map.key)
//                        showDialog(activity, map.key)
                    }
                    break
                }
            }
            if (isAllGrant) {
                callback()
            }
        }
    )
    return {
        if (permissionState.allPermissionsGranted) {
            callback()
        } else {
            shouldShowRationale = permissionState.shouldShowRationale
            permissionState.launchMultiplePermissionRequest()
        }
    }
}