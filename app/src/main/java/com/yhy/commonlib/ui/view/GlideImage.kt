package com.yhy.commonlib.ui.view

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

/**
 * desc:
 **
 * user: xujj
 * time: 2025/2/8 14:24
 **/

@Composable
fun GlideImage(
    modifier: Modifier = Modifier,
    string: String,
    @DrawableRes placeholder: Int? = null,
    @DrawableRes error: Int? = null,
    size: Int? = null,
    centerCrop: Boolean = true,
    transform: Transformation<Bitmap>? = null
) {
    val context = LocalContext.current
    var drawable by remember {
        mutableStateOf<Drawable?>(null)
    }

    LaunchedEffect(string) {
        val builder = Glide.with(context.applicationContext).load(string)
        placeholder?.let { builder.placeholder(it) }
        error?.let { builder.error(it) }
        size?.let { builder.override(it) }
        transform?.let { builder.apply(RequestOptions.bitmapTransform(it)) }
        builder.into(object : CustomTarget<Drawable>() {
            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable>?
            ) {
                drawable = resource
            }

            override fun onLoadCleared(placeholder: Drawable?) {

            }

            override fun onLoadStarted(placeholder: Drawable?) {
                super.onLoadStarted(placeholder)
                drawable = placeholder
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                drawable = errorDrawable
            }
        })
    }

    AndroidView(
        modifier = modifier.clipToBounds(),
        factory = {
            ImageView(it).also {
                if (centerCrop) {
                    it.scaleType = ImageView.ScaleType.CENTER_CROP
                }
            }
        },
        update = {
            it.setImageDrawable(drawable)
        }
    )
}