package com.yhy.commonlib.main

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.lib.automix.ui.AutoMixActivity
import com.yhy.commonlib.R
import com.yhy.commonlib.ui.theme.CommonLibTheme
import com.yhy.commonlib.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()
    private var permissionUtils: PermissionUtils? = null

    @OptIn(ExperimentalEncodingApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionUtils = PermissionUtils(this)
        permissionUtils?.launch(PermissionUtils.audioPermission) {
            viewModel.loadMusicList()
        }

        enableEdgeToEdge()
//        setContent {
//            CommonLibTheme {
//                MainNav(viewModel)
//            }
//        }
        setContentView(R.layout.activity_main)

        lifecycleScope.launch(Dispatchers.IO) {
//            Log.d("xxx", "onCreate1: " + SearchApiService.search(w = "赵希予"))
//            Log.d("xxx", "onCreate: " + TopListApiService.getTopList())
//            Log.d("xxx", "onCreate: " + TopListApiService.getTopListDetail(4))
//            Log.d("xxx", "onCreate: " + SearchApiService.smartBox("赵希予"))
//            Log.d(
//                "xxx", "onCreate2: " + MusicApiService.getMusicUrls(
//                    arrayOf(
//                        "002K1Rvs4I3bE9",
//                        "001PkDD90Y4uG2"
//                    )
//                )
//            )
//            Log.d("xxx", "onCreate3: " + MusicApiService.getSingerList())
//            Log.d("xxx", "onCreate4: " + MusicApiService.getSingerSongList("002iqR7z2kK6vb", 0))
//            Log.d("xxx", "onCreate5: " + TopListApiService.getAlbumInfo("001B7hGl2Rc1ZS"))
//            Log.d("xxx", "onCreate: " + KuwoApiService.getPicShortList("赵希予"))

//            val bytes = Base64.decode(
//                "W2lkOiQwMDAwMDAwMF0NClt0aTrpnZLoirHnk7ddDQpbYXI65ZGo5p2w5LymXQ0KW2FsOuaIkeW+iOW/mV0NCltieTpdDQpbMDA6MDAuMDBd6Z2S6Iqx55O3IC0g5ZGo5p2w5LymDQpbMDA6MDUuNDld6K+N77ya5pa55paH5bGxDQpbMDA6MTAuOThd5puy77ya5ZGo5p2w5LymDQpbMDA6MTYuNDdd57yW5puy77ya6ZKf5YW05rCRDQpbMDA6MjEuOTdd57Sg6IOa5Yu+5YuS5Ye66Z2S6Iqx56yU6ZSL5rWT6L2s5rehDQpbMDA6MjYuMjJd55O26Lqr5o+P57uY55qE54mh5Li55LiA5aaC5L2g5Yid5aaGDQpbMDA6MzAuNzBd5YaJ5YaJ5qqA6aaZ6YCP6L+H56qX5b+D5LqL5oiR5LqG54S2DQpbMDA6MzUuMThd5a6j57q45LiK6LWw56yU6Iez5q2k5pCB5LiA5Y2KDQpbMDA6MzkuNjJd6YeJ6Imy5riy5p+T5LuV5aWz5Zu+6Z+15ZGz6KKr56eB6JePDQpbMDA6NDQuMDNd6ICM5L2g5auj54S255qE5LiA56yR5aaC5ZCr6Iue5b6F5pS+DQpbMDA6NDguNDVd5L2g55qE576O5LiA57yV6aOY5pWjDQpbMDA6NTAuOTJd5Y675Yiw5oiR5Y675LiN5LqG55qE5Zyw5pa5DQpbMDA6NTcuMzVd5aSp6Z2S6Imy562J54Of6ZuoIOiAjOaIkeWcqOetieS9oA0KWzAxOjAxLjc1XeeCiueDn+iiheiiheWNh+i1tyDpmpTmsZ/ljYPkuIfph4wNClswMTowNi4yNl3lnKjnk7blupXkuabmsYnpmrbku7/liY3mnJ3nmoTpo5jpgLgNClswMToxMC43MV3lsLHlvZPmiJHkuLrpgYfop4HkvaDkvI/nrJQNClswMToxNS4xM13lpKnpnZLoibLnrYnng5/pm6gg6ICM5oiR5Zyo562J5L2gDQpbMDE6MTkuNTld5pyI6Imy6KKr5omT5o2e6LW3IOaZleW8gOS6hue7k+WxgA0KWzAxOjI0LjA1XeWmguS8oOS4lueahOmdkuiKseeTt+iHqumhvuiHque+juS4vQ0KWzAxOjI3Ljk3XeS9oOecvOW4pueskeaEjw0KWzAxOjUwLjcyXeiJsueZveiKsemdkueahOmUpumypOi3g+eEtuS6jueil+W6lQ0KWzAxOjU1LjE3XeS4tOaRueWui+S9k+iQveasvuaXtuWNtOaDpuiusOedgOS9oA0KWzAxOjU5LjYzXeS9oOmakOiXj+WcqOeqkeeDp+mHjOWNg+W5tOeahOenmOWvhg0KWzAyOjA0LjEzXeaegee7huiFu+eKueWmgue7o+iKsemSiOiQveWcsA0KWzAyOjA4LjQ4XeW4mOWkluiKreiVieaDuemqpOmbqOmXqOeOr+aDuemTnOe7vw0KWzAyOjEyLjk3XeiAjOaIkei3r+i/h+mCo+axn+WNl+Wwj+mVh+aDueS6huS9oA0KWzAyOjE3LjMzXeWcqOazvOWiqOWxseawtOeUu+mHjA0KWzAyOjE5Ljg4XeS9oOS7juWiqOiJsua3seWkhOiiq+makOWOuw0KWzAyOjI2LjI2XeWkqemdkuiJsuetieeDn+mbqCDogIzmiJHlnKjnrYnkvaANClswMjozMC42OV3ngorng5/oooXoooXljYfotbcg6ZqU5rGf5Y2D5LiH6YeMDQpbMDI6MzUuMTdd5Zyo55O25bqV5Lmm5rGJ6Zq25Lu/5YmN5pyd55qE6aOY6YC4DQpbMDI6MzkuNjJd5bCx5b2T5oiR5Li66YGH6KeB5L2g5LyP56yUDQpbMDI6NDQuMDhd5aSp6Z2S6Imy562J54Of6ZuoIOiAjOaIkeWcqOetieS9oA0KWzAyOjQ4LjQ2XeaciOiJsuiiq+aJk+aNnui1tyDmmZXlvIDkuobnu5PlsYANClswMjo1Mi45OF3lpoLkvKDkuJbnmoTpnZLoirHnk7foh6rpob7oh6rnvo7kuL0NClswMjo1Ni44NF3kvaDnnLzluKbnrJHmhI8NClswMzowMS43OV3lpKnpnZLoibLnrYnng5/pm6gg6ICM5oiR5Zyo562J5L2gDQpbMDM6MDYuMjNd54KK54Of6KKF6KKF5Y2H6LW3IOmalOaxn+WNg+S4h+mHjA0KWzAzOjEwLjcyXeWcqOeTtuW6leS5puaxiematuS7v+WJjeacneeahOmjmOmAuA0KWzAzOjE1LjE1XeWwseW9k+aIkeS4uumBh+ingeS9oOS8j+eslA0KWzAzOjE5LjY1XeWkqemdkuiJsuetieeDn+mbqCDogIzmiJHlnKjnrYnkvaANClswMzoyNC4wNV3mnIjoibLooqvmiZPmjZ7otbcg5pmV5byA5LqG57uT5bGADQpbMDM6MjguNDVd5aaC5Lyg5LiW55qE6Z2S6Iqx55O36Ieq6aG+6Ieq576O5Li9DQpbMDM6MzIuNDBd5L2g55y85bim56yR5oSPDQo="
//            )
//            Log.d("xxx", "onCreate: " + String(bytes, Charsets.UTF_8))
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = {
            AutoMixActivity.actionStart(context)
        }) {
            Text(text = "Hello $name!")

        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CommonLibTheme {
        Greeting("Android")
    }
}