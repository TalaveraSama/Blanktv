package app.blanktv.tv.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.blanktv.tv.data.db.ChannelEntity
import app.blanktv.tv.data.db.PlaylistEntity
import app.blanktv.tv.di.AppContainer
import app.blanktv.tv.domain.MediaKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Library-facing state for every browse screen.
 *
 * One ViewModel backs Home/Live/Movies/Series/Favourites/Search/Settings on purpose: they all read
 * the same Room flows, and splitting them would mean five subscriptions to the same query and five
 * copies of the channel list in memory.
 */
class LibraryViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.library

    val playlists: StateFlow<List<PlaylistEntity>> =
        repo.playlists().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val live: StateFlow<List<ChannelEntity>> =
        repo.channels(MediaKind.Live).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favourites: StateFlow<List<ChannelEntity>> =
        repo.favourites().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favouriteIds: StateFlow<List<Long>> =
        repo.favouriteIds().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- Movies / Series: group rail drives the grid -------------------------------
    val movieGroups: StateFlow<List<String>> =
        repo.groups(MediaKind.Movie).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val seriesGroups: StateFlow<List<String>> =
        repo.groups(MediaKind.Series).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _movieGroup = MutableStateFlow<String?>(null)
    val movieGroup: StateFlow<String?> = _movieGroup

    val movies: StateFlow<List<ChannelEntity>> = _movieGroup
        .flatMapLatest { group ->
            if (group == null) repo.channels(MediaKind.Movie)
            else repo.byGroup(MediaKind.Movie, group)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _seriesGroup = MutableStateFlow<String?>(null)
    val seriesGroup: StateFlow<String?> = _seriesGroup

    val series: StateFlow<List<ChannelEntity>> = _seriesGroup
        .flatMapLatest { group ->
            if (group == null) repo.channels(MediaKind.Series)
            else repo.byGroup(MediaKind.Series, group)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- Search --------------------------------------------------------------------
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val searchResults: StateFlow<List<ChannelEntity>> = _query
        .debounce(SEARCH_DEBOUNCE_MS)
        .flatMapLatest { q ->
            val trimmed = q.trim()
            if (trimmed.isEmpty()) flowOf(emptyList()) else repo.search(trimmed)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- One-shot import state ------------------------------------------------------
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setMovieGroup(group: String?) {
        _movieGroup.value = group
    }

    fun setSeriesGroup(group: String?) {
        _seriesGroup.value = group
    }

    fun addM3u(name: String, url: String) = runImport { repo.addM3u(name, url) }

    fun addXtream(name: String, baseUrl: String, username: String, password: String) =
        runImport { repo.addXtream(name, baseUrl, username, password) }

    fun addStalker(name: String, portalUrl: String, macAddress: String) =
        runImport { repo.addStalker(name, portalUrl, macAddress) }

    fun resync(playlistId: Long) = runImport { repo.resync(playlistId) }

    private fun runImport(block: suspend () -> Result<Int>) {
        viewModelScope.launch {
            _busy.value = true
            block()
                .onSuccess { count -> _status.value = "Imported $count entries" }
                .onFailure { error -> _status.value = error.message ?: "Import failed" }
            _busy.value = false
        }
    }

    fun removePlaylist(playlistId: Long) {
        viewModelScope.launch { repo.remove(playlistId) }
    }

    fun toggleFavourite(channelId: Long, currentlyFavourite: Boolean) {
        viewModelScope.launch { repo.setFavourite(channelId, !currentlyFavourite) }
    }

    fun clearStatus() {
        _status.value = null
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
