package com.yhy.commonlib.ui.view

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.yhy.commonlib.R

/**
 * desc:
 **
 * user: xujj
 * time: 2025/5/13 11:42
 **/
class AndroidComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
    private val content = mutableStateOf<(@Composable () -> Unit)?>(null)

    @Suppress("RedundantVisibilityModifier")
    protected override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    @Composable
    override fun Content() {
        content.value?.invoke()
    }

    override fun getAccessibilityClassName(): CharSequence {
        return javaClass.name
    }

    /**
     * Set the Jetpack Compose UI content for this view.
     * Initial composition will occur when the view becomes attached to a window or when
     * [createComposition] is called, whichever comes first.
     */
    fun setContent(content: @Composable () -> Unit) {
        shouldCreateCompositionOnAttachedToWindow = true
        this.content.value = content
        if (isAttachedToWindow) {
            createComposition()
        }
    }

    init {
        setContent()
    }

    fun setContent() {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
//            CommonLibTheme {
////            Box(modifier = Modifier
////                .fillMaxSize()
////                .background(Purple80, RoundedCornerShape(20.dp)))
////                MainNav()
//            }


            val bitmapDisc = ImageBitmap.imageResource(id = R.drawable.img_disk)
            val bitmapCover = ImageBitmap.imageResource(id = R.drawable.fxsh)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                val discSize = size.width
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
        }
    }
}