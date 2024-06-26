package com.mymusic.modules.dashboard

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.QuerySnapshot
import com.mymusic.AppContainer
import com.mymusic.modules.music.Music
import com.mymusic.modules.musicplayer.MusicPlayerService
import com.mymusic.modules.musicplayer.MusicPlayerState
import com.mymusic.modules.search.MusicSearch
import com.mymusic.util.Resource
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val dashboardService: DashboardService = DashboardService(),
    private val musicPlayerService: MusicPlayerService = AppContainer.musicPlayerService,
) : ViewModel() {

    val refreshRecommendation = MutableLiveData<Boolean>()
    val musicHistoryList = MutableLiveData<List<Music>>()
    val recommendationList = dashboardService.recommendationFlow.asLiveData()
    val currentMusic = MutableLiveData<Music>()
    val playerState = MutableLiveData(MusicPlayerState.PAUSE)
    private val histories = mutableListOf<String>()

    val searchList = MutableLiveData<Resource<List<MusicSearch>>>()

    private val musicListener = { music: Music ->
        currentMusic.postValue(music)
    }

    private val stateListener: (MusicPlayerState) -> Unit = { musicPlayerState1: MusicPlayerState ->
        viewModelScope.launch {
            playerState.postValue(musicPlayerState1)
            when (musicPlayerState1) {
                MusicPlayerState.PREPARED -> {
                    val music = currentMusic.value
                    if (music != null) {
                        dashboardService.addHistory(music.id)
                    }
                }
                MusicPlayerState.PLAY -> {
                }
                MusicPlayerState.PAUSE -> {
                }
                MusicPlayerState.LOADING -> {
                }
                MusicPlayerState.COMPLETED -> {
                    dashboardService.startNextRecommendation()
                }
            }
        }
    }

    private val musicHistoryListener: EventListener<QuerySnapshot> =
        EventListener<QuerySnapshot> { value, error ->
            viewModelScope.launch {
                val names = mutableListOf<String>()
                val musics = mutableListOf<Music>()
                if (error == null) {
                    value?.forEach { queryDocumentSnapshot ->
                        val music =
                            AppContainer.musicRepository.getMusicById(queryDocumentSnapshot.id)
                        if (music != null) {
                            musics.add(music)
                            names.add(music.name)
                        }
                    }
                }
                musicHistoryList.value = musics
                histories.clear()
                histories.addAll(names)
            }
        }

    init {
        viewModelScope.launch {
            recommendationList.observeForever {
                refreshRecommendation.value = false
            }
            dashboardService.attachHistoryListener(musicHistoryListener)
            musicPlayerService.stateDataListener.addAndListen(stateListener)
            musicPlayerService.musicDataListener.add(musicListener)
        }
    }

    override fun onCleared() {
        musicPlayerService.clear()
        dashboardService.removeHistoryListener()
        super.onCleared()
    }

    fun search(search: String) {
        viewModelScope.launch {
            searchList.value = Resource(dashboardService.getSearches(search))
        }
    }

    fun start(musicSearch: MusicSearch) {
        viewModelScope.launch {
            val music: Music? = dashboardService.getMusic(musicSearch)
            if (music != null) {
                musicPlayerService.start(music)
            }
        }
    }

    fun start(music: Music) {
        viewModelScope.launch {
            musicPlayerService.start(music)
        }
    }

    fun play() {
        viewModelScope.launch {
            musicPlayerService.play()
        }
    }

    fun pause() {
        viewModelScope.launch {
            musicPlayerService.pause()
        }
    }

    fun logOut() {
        viewModelScope.launch {
            dashboardService.logOut()
            musicPlayerService.clear()
        }
    }

    fun refreshRecommendation() {
        viewModelScope.launch {
            refreshRecommendation.value = true
            dashboardService.refreshRecommendationList(histories)
        }
    }
}