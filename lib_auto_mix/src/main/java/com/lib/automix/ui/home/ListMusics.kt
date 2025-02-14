package com.lib.automix.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lib.automix.R
import com.lib.media.entity.Music
import kotlinx.coroutines.launch

/**
 * desc:
 **
 * user: xujj
 * time: 2024/7/23 17:53
 **/

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListMusics(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    musicItems: State<List<Music>?>,
    onItemClick: (Int) -> Unit = {},
    onItemDelete: (Int) -> Unit = {}
) {
    val items by musicItems
    items?.let { items ->

        val coroutineScope = rememberCoroutineScope()
        val columnHeight = remember {
            Animatable(0f)
        }
        val itemHeight = with(LocalDensity.current) {
            70.dp.toPx()
        }

        LazyColumn(
            modifier = modifier
                .padding(horizontal = 15.dp)
                .onSizeChanged {
                    coroutineScope.launch {
                        columnHeight.animateTo(targetValue = it.height.toFloat())
                    }
                },
            state = lazyListState,
            contentPadding = PaddingValues(
                vertical = with(LocalDensity.current) {
                    (columnHeight.value / 2).toDp()
                }
            ),
            reverseLayout = true
        ) {
            itemsIndexed(items, key = { index, item -> "$index ${item.id}" }) { index, item ->
                ItemMusic(
//                    modifier = Modifier.animateItemPlacement(),
                    item = item,
                    onItemClick = {
                        onItemClick(index)
                    },
                    onItemDelete = {
                        onItemDelete(index)
                    })
            }
        }

        if (lazyListState.isScrollInProgress) {
            DisposableEffect(Unit) {
                onDispose {
                    val index =
                        if (lazyListState.firstVisibleItemScrollOffset > itemHeight / 2) {
                            lazyListState.firstVisibleItemIndex + 1
                        } else {
                            lazyListState.firstVisibleItemIndex
                        }
                    coroutineScope.launch {
                        if (lazyListState.firstVisibleItemScrollOffset > 0 &&
                            lazyListState.firstVisibleItemScrollOffset < itemHeight
                        ) {
                            lazyListState.animateScrollToItem(index)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemMusic(
    modifier: Modifier = Modifier,
    item: Music,
    onItemClick: () -> Unit = {},
    onItemDelete: () -> Unit = {}
) {

    val imageScale = remember {
        mutableStateOf(ContentScale.Inside)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(5.dp))
            .clickable {
                onItemClick()
            }
            .padding(start = 15.dp, end = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0x1AFFFFFF)),
            model = item.album,
            contentDescription = null,
            placeholder = painterResource(R.drawable.automix_ic_music),
            error = painterResource(R.drawable.automix_ic_music),
            fallback = painterResource(R.drawable.automix_ic_music),
            contentScale = imageScale.value,
            onLoading = {
                imageScale.value = ContentScale.Inside
            },
            onError = {
                imageScale.value = ContentScale.Inside
            },
            onSuccess = {
                imageScale.value = ContentScale.Crop
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 15.dp, end = 5.dp)
        ) {
            Text(
                text = "${item.title}",
                fontSize = 15.sp,
                color = colorResource(id = R.color.am_text_color),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.artist}",
                fontSize = 12.sp,
                color = colorResource(id = R.color.am_subtext_color),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onItemDelete) {
            Icon(
                imageVector = Icons.Rounded.Clear,
                contentDescription = null,
                tint = colorResource(id = R.color.am_text_color)
            )
        }
    }
}

@Preview
@Composable
private fun ListMusicsPreview() {
//    Column(
//        Modifier
//            .background(colorResource(id = R.color.am_background))
//    ) {
//        ItemMusic(item = Music.example("xxx1"))
//        ItemMusic(item = Music.example("xxx2"))
//        ItemMusic(item = Music.example("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx3"))
//    }

    ListMusics(
        modifier = Modifier
            .background(colorResource(id = R.color.am_background)),
        musicItems = remember {
            mutableStateOf(
                listOf(
                    Music.example("xxx1"),
                    Music.example("xxx2"),
                    Music.example("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx3")
                )
            )
        })
}