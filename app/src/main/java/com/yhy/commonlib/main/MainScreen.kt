package com.yhy.commonlib.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lib.media.entity.Music
import com.lib.media.utils.MediaUtils
import com.yhy.commonlib.R
import com.yhy.commonlib.ui.dialog.buildColorPicker
import com.yhy.commonlib.ui.theme.Purple80
import com.yhy.commonlib.ui.view.GlideImage
import com.yhy.lib_retrofit.service.DeepSeekApiService
import com.yhy.lib_web_pic.utils.WebPicUtils
import kotlinx.coroutines.launch

/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/7 10:15
 **/

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    navigateToWaveform: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    val musicList by viewModel.musicListFlow.collectAsStateWithLifecycle()
//    var newList by remember { mutableStateOf(musicList ?: listOf()) }

    var picUrls by remember { mutableStateOf<List<String>>(listOf()) }
    LaunchedEffect(Unit) {
        picUrls = WebPicUtils.getBingPicUrls("赵希予")
    }

    val colorPickerDialogState = buildColorPicker()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        LazyColumn {
//            picUrls.let {
//                items(it) {
//                    GlideImage(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .aspectRatio(1f),
//                        string = it,
//                        placeholder = R.drawable.ic_launcher_background,
//                        error = R.drawable.ic_launcher_background
//                    )
//                }
//            }
            musicList?.let {
                itemsIndexed(it, key = { index, item -> "$index ${item.uuid}" }) { index, item ->
                    MusicItem(item) {
                        navigateToWaveform(item.data)
//                        newList -= item

//                        colorPickerDialogState.show()
//                        scope.launch {
//                            DeepSeekApiService.request(item.name + "-" + item.artist)
//                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MusicItem(
    item: Music,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var coverUrl by remember {
        mutableStateOf(
            WebPicUtils.getPicUrl(
                context,
                item.id,
                item.albumId
            )
        )
    }
    var coverUrl1 by remember { mutableStateOf(MediaUtils.getAlbumArtUri(item.albumId).toString()) }
    var coverUrl2 by remember { mutableStateOf(MediaUtils.getAlbumArtUri(item.albumId).toString()) }
    var coverUrl3 by remember { mutableStateOf(MediaUtils.getAlbumArtUri(item.albumId).toString()) }
    LaunchedEffect(item) {
//        val webPic = WebPicUtils.getWebPicUrl(item.name, item.artist)
//        Log.d("xxx", "MusicItem: "+item.name+" "+webPic)
//        coverUrl = webPic.first
//        coverUrl1 = WebPicUtils.getKuwoPicUrl(item.name, item.artist)
//        coverUrl2 = WebPicUtils.getQQPicUrl(item.name, item.artist)
//        coverUrl3 = WebPicUtils.getBingPicUrl(item.name, item.artist)
    }

    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        scale *= zoomChange
        rotation += rotationChange
        offset += offsetChange
//        offset += Offset(offsetChange.x * scale, offsetChange.y * scale)
    }

    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                rotationZ = rotation,
                translationX = offset.x,
                translationY = offset.y
            )
            .transformable(state = state)
            .background(Purple80.copy(0.3f))
            .padding(horizontal = 15.dp, vertical = 7.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlideImage(
            modifier = Modifier
                .size(50.dp)
                .clickable {
                    WebPicUtils.updatePicUrl(
                        context,
                        item.id,
                        item.albumId,
                        item.name,
                        item.artist,
                        callback = {
                            Log.d("xxx", "MusicItem: " + it)
                            coverUrl = it.first
                        }
                    )
                },
            string = coverUrl,
            placeholder = R.drawable.ic_launcher_background,
            error = R.drawable.ic_launcher_background
        )
//        GlideImage(
//            modifier = Modifier.size(50.dp),
//            string = coverUrl1,
//            placeholder = R.drawable.ic_launcher_background,
//            error = R.drawable.ic_launcher_background
//        )
//        GlideImage(
//            modifier = Modifier.size(50.dp),
//            string = coverUrl2,
//            placeholder = R.drawable.ic_launcher_background,
//            error = R.drawable.ic_launcher_background
//        )
//        GlideImage(
//            modifier = Modifier.size(50.dp),
//            string = coverUrl3,
//            placeholder = R.drawable.ic_launcher_background,
//            error = R.drawable.ic_launcher_background
//        )
        Column(
            modifier = Modifier
                .padding(start = 15.dp)
                .weight(1f)
        ) {
            Text(
                text = item.name,
                fontSize = 16.sp
            )
            Text(
                text = item.artist + "-" + item.album,
                fontSize = 12.sp
            )
        }
    }
}