package com.lib.automix.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lib.automix.R
import com.lib.automix.ui.AutoMixActivity
import com.lib.automix.ui.AutoMixViewModel
import com.lib.automix.ui.icons.Pause
import com.lib.automix.ui.icons.Shuffle
import com.lib.automix.ui.icons.SkipNext
import com.lib.automix.ui.view.ComposeDisk
import com.lib.automix.ui.view.ComposeToolbar
import com.lib.automix.ui.view.getPainter
import com.lib.automix.utils.getAccentColor
import com.lib.media.entity.Music

/**
 * desc:
 **
 * user: xujj
 * time: 2024/7/23 10:59
 **/

data class HomeState(
    val uiState: State<Int> = mutableIntStateOf(AutoMixViewModel.NO_PERMISSION),
    val currentMusic: State<Music?> = mutableStateOf(null),
    val transitionMusic: State<Music?> = mutableStateOf(null),
    val currentDeck: State<Boolean> = mutableStateOf(true),
    val playState: State<Boolean> = mutableStateOf(false),
    val currentPosition: State<Int> = mutableIntStateOf(0),
    val duration: State<Int> = mutableIntStateOf(0),
    val transitionState: State<Boolean> = mutableStateOf(false),
    val transitionPosition: State<Int> = mutableIntStateOf(0),
    val volumeProgress: State<Int> = mutableIntStateOf(0),
    val musicItems: State<List<Music>?> = mutableStateOf(null),
    val shuffleMode: State<Boolean> = mutableStateOf(false),
    val transitionNext: () -> Unit = {},
    val playOrPause: () -> Unit = {},
    val toggleShuffleMode: () -> Unit = {},
    val startTransition: (Int) -> Unit = {},
    val removeMusic: (Int) -> Unit = {},
    val onStartTrackingTouch: () -> Unit = {},
    val onProgressChanged: (Int) -> Unit = {},
    val onStopTrackingTouch: () -> Unit = {}
)

@Composable
fun AutoMixHome(
    viewModel: AutoMixViewModel = viewModel(LocalContext.current as AutoMixActivity),
    onNavigationClick: () -> Unit = {},
    onSettingClick: () -> Unit = {},
    requestPermission: () -> Unit = {},
    addItem: (Int) -> Unit = {}
) {
    val homeState = HomeState(
        uiState = viewModel.uiState.collectAsStateWithLifecycle(),
        currentMusic = remember { viewModel.currentMusic },
        transitionMusic = remember { viewModel.transitionMusic },
        currentDeck = remember { viewModel.currentDeck },
        playState = viewModel.playState.collectAsStateWithLifecycle(),
        currentPosition = viewModel.currentPosition.collectAsStateWithLifecycle(),
        duration = viewModel.duration.collectAsStateWithLifecycle(),
        transitionState = viewModel.transitionState.collectAsStateWithLifecycle(),
        transitionPosition = viewModel.transitionPosition.collectAsStateWithLifecycle(),
        volumeProgress = viewModel.volumeProgress.collectAsStateWithLifecycle(),
        musicItems = viewModel.musicItems.observeAsState(),
        shuffleMode = viewModel.shuffleMode.collectAsStateWithLifecycle(),
        transitionNext = viewModel::transitionNext,
        playOrPause = viewModel::playOrPause,
        toggleShuffleMode = viewModel::toggleShuffleMode,
        startTransition = viewModel::startTransition,
        removeMusic = viewModel::removeMusic,
        onStartTrackingTouch = viewModel::onStartTrackingTouch,
        onProgressChanged = viewModel::onProgressChanged,
        onStopTrackingTouch = viewModel::onStopTrackingTouch
    )
    AutoMixHome(
        homeState,
        onNavigationClick,
        onSettingClick,
        requestPermission,
        addItem
    )
}

@Composable
fun AutoMixHome(
    homeState: HomeState,
    onNavigationClick: () -> Unit = {},
    onSettingClick: () -> Unit = {},
    requestPermission: () -> Unit = {},
    addItem: (Int) -> Unit = {}
) {
    val uiState by homeState.uiState
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorResource(R.color.am_background),
        topBar = {
            val textColor = colorResource(R.color.am_text_color)
            ComposeToolbar(
                containerColor = Color.Transparent,
                titleResource = R.string.stop_auto_mix,
                titleSize = 18.sp,
                titleColor = textColor,
                titleWeight = FontWeight.Bold,
                navigationIconColor = textColor,
                actionsIconColor = textColor,
                navigationPainter = getPainter(imageVector = Icons.AutoMirrored.Default.ArrowBack),
                action1Painter = getPainter(imageVector = Icons.Default.Settings),
                onNavigationClick = onNavigationClick,
                onAction1Click = onSettingClick,
            )
        }
    ) {
        when (uiState) {
            AutoMixViewModel.NO_PERMISSION -> {
                NoPermissionBody(
                    modifier = Modifier
                        .padding(it)
                        .fillMaxSize(),
                    requestPermission = requestPermission
                )
            }

            AutoMixViewModel.HAS_PERMISSION -> {
                HomeBody(
                    modifier = Modifier
                        .padding(it)
                        .fillMaxSize(),
                    homeState = homeState,
                    addItem = addItem
                )
            }
        }
    }
}

@Composable
private fun NoPermissionBody(
    modifier: Modifier = Modifier,
    requestPermission: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.storage_permission_msg),
            color = colorResource(id = R.color.am_subtext_color),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(25.dp))
        Text(
            text = stringResource(id = R.string.authorize),
            color = colorResource(id = R.color.am_text_color),
            fontSize = 18.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(color = colorResource(id = R.color.colorAccent))
                .clickable(onClick = requestPermission)
                .padding(horizontal = 25.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun HomeBody(
    modifier: Modifier,
    homeState: HomeState,
    addItem: (Int) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val constraints = ConstraintSet {
        val endGuideLine = createGuidelineFromEnd(0.5f)
        val disk = createRefFor("disk")
        val titleColumn = createRefFor("titleColumn")
        val buttonRow = createRefFor("buttonRow")
        val btnAddMusic = createRefFor("btnAddMusic")
        val listMusics = createRefFor("listMusics")
        constrain(disk) {
            top.linkTo(parent.top, 10.dp)
            bottom.linkTo(titleColumn.top, 10.dp)
            start.linkTo(parent.start, 30.dp)
            end.linkTo(endGuideLine, 30.dp)
            height = Dimension.ratio("1:1")
            width = Dimension.ratio("1:1")
        }
        constrain(titleColumn) {
            start.linkTo(parent.start, 30.dp)
            end.linkTo(endGuideLine, 30.dp)
            bottom.linkTo(parent.bottom)
        }
        constrain(buttonRow) {
            start.linkTo(endGuideLine, 30.dp)
            end.linkTo(parent.end, 30.dp)
            bottom.linkTo(parent.bottom)
        }
        constrain(btnAddMusic) {
            top.linkTo(parent.top)
            bottom.linkTo(titleColumn.top)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        }
        constrain(listMusics) {
            top.linkTo(parent.top)
            bottom.linkTo(titleColumn.top)
            start.linkTo(endGuideLine)
            end.linkTo(parent.end)
            width = Dimension.fillToConstraints
            height = Dimension.fillToConstraints
        }
    }
    ConstraintLayout(
        constraintSet = constraints,
        modifier = modifier
    ) {
        ComposeDisk(
            modifier = Modifier
                .layoutId("disk"),
            homeState = homeState
        )
        TitleColumn(
            modifier = Modifier
                .layoutId("titleColumn")
                .height(64.dp),
            homeState = homeState
        )
        ButtonRow(
            modifier = Modifier
                .layoutId("buttonRow")
                .height(64.dp),
            homeState = homeState
        )
        ListMusics(
            modifier = Modifier
                .layoutId("listMusics"),
            lazyListState = lazyListState,
            musicItems = homeState.musicItems,
            onItemClick = {
                homeState.startTransition(it)
            },
            onItemDelete = {
                homeState.removeMusic(it)
            }
        )
        BtnAddMusic(
            modifier = Modifier
                .layoutId("btnAddMusic"),
            currentDeck = homeState.currentDeck.value,
            onClick = {
                if (!lazyListState.isScrollInProgress) {
                    val index = if (lazyListState.firstVisibleItemScrollOffset > 0) {
                        lazyListState.firstVisibleItemIndex + 1
                    } else {
                        lazyListState.firstVisibleItemIndex
                    }
                    addItem(index)
                }
            })
    }
}

@Composable
private fun TitleColumn(
    modifier: Modifier = Modifier,
    homeState: HomeState
) {
    val context = LocalContext.current
    val currentColor = Color(getAccentColor(context, homeState.currentDeck.value))
    val anotherColor = Color(getAccentColor(context, !homeState.currentDeck.value))
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        homeState.currentMusic.value?.let {
            Text(
                text = it.title,
                color = currentColor,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        homeState.transitionMusic.value?.let {
            Text(
                text = it.title,
                color = anotherColor,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ButtonRow(
    modifier: Modifier = Modifier,
    homeState: HomeState
) {
    val context = LocalContext.current
    val currentColor = Color(getAccentColor(context, homeState.currentDeck.value))
    val playState by homeState.playState
    val shuffleMode by homeState.shuffleMode

    val constraints = ConstraintSet {
        val btnPlay = createRefFor("btnPlay")
        val btnShuffle = createRefFor("btnShuffle")
        val btnNext = createRefFor("btnNext")
        constrain(btnPlay) {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
        }
        constrain(btnShuffle) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            end.linkTo(btnPlay.start, 15.dp)
        }
        constrain(btnNext) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            start.linkTo(btnPlay.end, 15.dp)
        }
    }
    ConstraintLayout(
        constraintSet = constraints,
        modifier = modifier
    ) {
        Image(
            imageVector = Icons.Rounded.Shuffle,
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                if (shuffleMode) {
                    currentColor
                } else {
                    Color.White
                }
            ),
            modifier = Modifier
                .layoutId("btnShuffle")
                .clip(CircleShape)
                .background(Color(0x1AFFFFFF), CircleShape)
                .clickable {
                    homeState.toggleShuffleMode()
                }
                .padding(6.dp)
        )
        Image(
            imageVector = if (playState) {
                Icons.Rounded.Pause
            } else {
                Icons.Rounded.PlayArrow
            },
            contentDescription = null,
            colorFilter = ColorFilter.tint(currentColor),
            modifier = Modifier
                .layoutId("btnPlay")
                .size(50.dp)
                .clip(CircleShape)
                .border(1.5.dp, currentColor, CircleShape)
                .clickable {
                    homeState.playOrPause()
                }
                .padding(6.dp)
        )
        if (!homeState.transitionState.value) {//过渡中隐藏
            Image(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = null,
                colorFilter = ColorFilter.tint(currentColor),
                modifier = Modifier
                    .layoutId("btnNext")
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF), CircleShape)
                    .clickable {
                        homeState.transitionNext()
                    }
                    .padding(6.dp)
            )
        }
    }
}

@Composable
private fun BtnAddMusic(
    modifier: Modifier = Modifier,
    currentDeck: Boolean,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentColor = Color(getAccentColor(context, currentDeck))
    Image(
        imageVector = Icons.Rounded.Add,
        contentDescription = null,
        colorFilter = ColorFilter.tint(currentColor),
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .border(1.5.dp, currentColor, CircleShape)
            .clickable {
                onClick()
            }
            .padding(6.dp)
    )
}

@Preview(name = "home", device = "spec:shape=Normal,width=640,height=360,unit=dp,dpi=480")
@Composable
private fun AutoMixHomePreview() {
    AutoMixHome(HomeState(uiState = remember { mutableIntStateOf(1) }))
}

@Preview(name = "no permission", device = "spec:shape=Normal,width=640,height=360,unit=dp,dpi=480")
@Composable
private fun AutoMixHomePreview2() {
    AutoMixHome(HomeState(uiState = remember { mutableIntStateOf(0) }))
}