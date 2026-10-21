package app.blanktv.tv.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.blanktv.tv.data.db.ChannelEntity
import app.blanktv.tv.di.AppContainer
import app.blanktv.tv.domain.MediaKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Playback state for the full-screen player.
 *
 * Keeps the live channel list in memory so CHANNEL_UP / CHANNEL_DOWN can zap without a database
 * round trip per key press; the list itself stays a Room flow, so a sync mid-watch updates it.
 */
class PlayerViewModel(private val container: AppContainer) : ViewModel() {

    private val _current = MutableStateFlow<ChannelEntity?>(null)
    val current: StateFlow<ChannelEntity?> = _current

    private var liveChannels: List<ChannelEntity> = emptyList()

    init {
        viewModelScope.launch {
            container.library.channels(MediaKind.Live).collect { liveChannels = it }
        }
    }

    fun open(channelId: Long) {
        viewModelScope.launch {
            val channel = container.library.findChannel(channelId) ?: return@launch
            _current.value = channel
            container.playback.play(channel)
        }
    }

    /** Zap by [delta] positions through the live list, wrapping at both ends. */
    fun step(delta: Int) {
        val list = liveChannels
        val shown = _current.value ?: return
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.id == shown.id }
        if (index < 0) return
        val next = list[(index + delta % list.size + list.size) % list.size]
        open(next.id)
    }

    override fun onCleared() {
        container.playback.stop()
        super.onCleared()
    }
}
