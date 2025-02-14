package com.lib.automix.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lib.automix.ui.theme.AutoMixTheme
import com.lib.automix.utils.AutoMixConfigUtils
import com.lib.automix.utils.PermissionUtils
import com.lib.automix.utils.isServiceRunning
import com.lib.media.entity.Music
import kotlinx.coroutines.launch

/**
 * desc:
 **
 * user: xujj
 * time: 2024/7/23 10:31
 **/
class AutoMixActivity : ComponentActivity() {

    private val autoMixViewModel by viewModels<AutoMixViewModel>()

    private var permissionUtils: PermissionUtils? = null

    private var launcher: ActivityResultLauncher<Intent>? = null

    companion object {
        fun actionStart(context: Context) {
            val intent = Intent(context, AutoMixActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionUtils = PermissionUtils(this)
        permissionUtils?.create()

        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK && it.data != null) {
                val music = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    it.data!!.getParcelableExtra("music", Music::class.java)
                } else {
                    it.data!!.getParcelableExtra("music")
                }
                val index = it.data!!.getIntExtra("index", 0)
                if (music != null) {
                    autoMixViewModel.insertMusic(index, music)
                }
            }
        }

        setContent {
            AutoMixTheme(isFullScreen = true) {
                AutoMixApp(
                    requestPermission = {
                        permissionUtils?.launch()
                    },
                    addItem = { index ->
                        val libraryIntent = AutoMixConfigUtils.getConfig()
                            ?.getLibraryIntent(this@AutoMixActivity)
                        libraryIntent?.let {
                            it.putExtra("isAutoMix", true)
                            it.putExtra("index", index)
                            launcher?.launch(it)
                        }
                    }
                )
            }
        }

        initService()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                autoMixViewModel.updateUiState()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        initService()
    }

    private fun initService() { //开启服务
        val serviceClass = AutoMixConfigUtils.getConfig()?.getServiceClass() ?: return
        try {
            if (!isServiceRunning(this, serviceClass.name)) {
                val intent = Intent(this, serviceClass)
                startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            autoMixViewModel.destroy()
        }
    }

}