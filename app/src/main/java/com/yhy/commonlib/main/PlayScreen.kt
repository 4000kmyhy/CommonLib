package com.yhy.commonlib.main

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lib.bass.BassPlayer
import com.lib.bass.utils.BassUtils
import com.yhy.commonlib.R
import com.yhy.commonlib.utils.rememberBlurImageBitmap
import com.yhy.lib_compose.utils.roundClickable
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * desc:
 **
 * user: xujj
 * time: 2025/9/23 10:59
 **/

class PlayViewModel(application: Application) : AndroidViewModel(application) {

    val player = BassPlayer()

    var currentPosition by mutableIntStateOf(0)
        private set
    var duration by mutableIntStateOf(0)
        private set
    var isPlaying by mutableStateOf(false)
        private set

    init {
        BassUtils.initBass()

        player.setDataSource(application, "fet.mp3")
    }

    fun playOrPause() {
        if (player.isPlaying()) {
            player.pause()
        } else {
            player.play()
            updateProgress()
        }
        isPlaying = player.isPlaying()
    }

    private fun updateProgress() {
        viewModelScope.launch(Dispatchers.Default) {
            while (player.isPlaying()) {
                currentPosition = player.getCurrentPosition()
                duration = player.getDuration()
//                delay(100)
            }
        }
    }

    fun seekTo(position: Int) {
        player.seekTo(position)
        updateProgress()
    }
}

@Composable
fun PlayScreen(
    viewModel: PlayViewModel = viewModel()
) {

    val blurImageBitmap by rememberBlurImageBitmap(
        string = "",
        defRes = R.drawable.fetzg,
        BlurTransformation(5, 5)
    )

    val bitmapDisc = ImageBitmap.imageResource(id = R.drawable.img_disk)
    val bitmapCover = ImageBitmap.imageResource(id = R.drawable.fetzg)

    val accentColor = Color(0xFFAE57FF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Color(0xFF303030))
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .padding(30.dp)
                .rotate(viewModel.currentPosition / 8f)
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(
                    Color.White.copy(0.05f),
                    CircleShape
                )
                .border(
                    width = 1.5.dp,
                    Color.White.copy(0.05f),
                    CircleShape
                )
                .padding(10.dp)
        ) {
            val discSize = size.width
//            blurImageBitmap?.let {
//                drawImage(
//                    image = it,
//                    dstSize = IntSize(discSize.toInt(), discSize.toInt())
//                )
//            }
            drawImage(
                image = bitmapDisc,
                dstSize = IntSize(discSize.toInt(), discSize.toInt())
            )


            val coverSize = discSize * 220f / 410f
            drawImage(
                image = bitmapCover,
                dstOffset = IntOffset(
                    ((discSize - coverSize) / 2).toInt(),
                    ((discSize - coverSize) / 2).toInt()
                ),
                dstSize = IntSize(coverSize.toInt(), coverSize.toInt())
            )
        }

        Text(
            text = "法厄同之歌",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Slider(
            modifier = Modifier.padding(30.dp),
            value = if (viewModel.duration > 0) {
                1f * viewModel.currentPosition / viewModel.duration
            } else {
                0f
            },
            onValueChange = {
                val position = viewModel.duration * it
                viewModel.seekTo(position.toInt())
            },
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor
            )
        )

        Icon(
            modifier = Modifier
                .padding(top = 50.dp)
                .size(70.dp)
                .roundClickable {
                    viewModel.playOrPause()
                },
            imageVector = if (viewModel.isPlaying) {
                Icons.Rounded.PauseCircle
            } else {
                Icons.Rounded.PlayCircle
            },
            contentDescription = null,
            tint = accentColor
        )
    }
}