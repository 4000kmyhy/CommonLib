package com.yhy.commonlib.utils

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import jp.wasabeef.glide.transformations.BlurTransformation

/**
 * desc:
 **
 * user: xujj
 * time: 2025/8/22 10:40
 **/


@Composable
fun rememberImageBitmap(
    string: String?,
    @DrawableRes defRes: Int
): MutableState<ImageBitmap?> {
    val context = LocalContext.current
    val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(string) {
        val thumbnail = Glide.with(context.applicationContext)
            .asBitmap()
            .load(defRes)
        Glide.with(context.applicationContext)
            .asBitmap()
            .load(string)
            .error(thumbnail)
            .transition(BitmapTransitionOptions.withCrossFade())
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    imageBitmap.value = resource.asImageBitmap()
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }
            })
    }
    return imageBitmap
}

@Composable
fun GlideImage(
    modifier: Modifier = Modifier,
    string: String?,
    @DrawableRes defRes: Int,
    @DrawableRes placeholder: Int? = null,
    @DrawableRes error: Int? = null,
    size: Int? = null,
    centerCrop: Boolean = true,
    transform: Transformation<Bitmap>? = null
) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(string) {
        val builder = Glide.with(context.applicationContext).asBitmap().load(string)
        placeholder?.let { builder.placeholder(it) }
        error?.let { builder.error(it) }
        size?.let { builder.override(it) }
        transform?.let { builder.apply(RequestOptions.bitmapTransform(it)) }
        builder.into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(
                resource: Bitmap,
                transition: Transition<in Bitmap>?
            ) {
                imageBitmap = resource.asImageBitmap()
            }

            override fun onLoadCleared(placeholder: Drawable?) {

            }

            override fun onLoadStarted(placeholder: Drawable?) {
                super.onLoadStarted(placeholder)
                imageBitmap = placeholder?.toBitmap()?.asImageBitmap()
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                imageBitmap = errorDrawable?.toBitmap()?.asImageBitmap()
            }
        })
    }

    Image(
        modifier = modifier.clipToBounds(),
        bitmap = imageBitmap ?: ImageBitmap.imageResource(defRes),
        contentDescription = null,
        contentScale = if (centerCrop) {
            ContentScale.Crop
        } else {
            ContentScale.Fit
        }
    )
}

@Composable
fun rememberBlurImageBitmap(
    string: String?,
    @DrawableRes defRes: Int,
    transform: BlurTransformation = BlurTransformation(30, 30)
): MutableState<ImageBitmap?> {
    val context = LocalContext.current
    val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(string) {
        val thumbnail = Glide.with(context.applicationContext)
            .asBitmap()
            .load(defRes)
            .transform(transform)
        Glide.with(context.applicationContext)
            .asBitmap()
            .load(string)
            .error(thumbnail)
            .transform(transform)
            .transition(BitmapTransitionOptions.withCrossFade())
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    imageBitmap.value = resource.asImageBitmap()
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }
            })
    }
    return imageBitmap
}

@Composable
fun BlurImage(
    modifier: Modifier = Modifier,
    string: String?,
    @DrawableRes defRes: Int,
    transform: BlurTransformation = BlurTransformation(30, 30)
) {
    val imageBitmap by rememberBlurImageBitmap(string, defRes, transform)
    imageBitmap?.let {
        Image(
            modifier = modifier.clipToBounds(),
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
    }
}