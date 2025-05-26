package com.yhy.commonlib.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lib.media.entity.Music
import com.lib.media.utils.MusicLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/7 10:16
 **/
class MainViewModel(application: Application) : AndroidViewModel(application) {

    val musicListFlow = MutableStateFlow<List<Music>?>(null)

    init {
        loadMusicList()
    }

    fun loadMusicList() {
        viewModelScope.launch(Dispatchers.IO) {
            musicListFlow.value = MusicLoader.getMusicList(getApplication())
        }
    }
}