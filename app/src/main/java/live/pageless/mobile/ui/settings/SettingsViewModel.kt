package live.pageless.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import live.pageless.mobile.data.local.PlayerSettings
import live.pageless.mobile.data.local.PlayerSettingsStore
import live.pageless.mobile.data.local.ThemeMode
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val store: PlayerSettingsStore,
    ) : ViewModel() {
        val settings: StateFlow<PlayerSettings> =
            store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerSettings())

        fun setUseChapterTrack(value: Boolean) {
            viewModelScope.launch { store.setUseChapterTrack(value) }
        }

        fun setJumpForward(seconds: Int) {
            viewModelScope.launch { store.setJumpForwardSeconds(seconds) }
        }

        fun setJumpBackward(seconds: Int) {
            viewModelScope.launch { store.setJumpBackwardSeconds(seconds) }
        }

        fun setBookmarkContext(seconds: Int) {
            viewModelScope.launch { store.setBookmarkContextSeconds(seconds) }
        }

        fun setShowTotalTrackOnNowPlaying(value: Boolean) {
            viewModelScope.launch { store.setShowTotalTrackOnNowPlaying(value) }
        }

        fun setShowChapterTrackOnNowPlaying(value: Boolean) {
            viewModelScope.launch { store.setShowChapterTrackOnNowPlaying(value) }
        }

        fun setShowChapterStartOnBookDetail(value: Boolean) {
            viewModelScope.launch { store.setShowChapterStartOnBookDetail(value) }
        }

        fun setShowChapterDurationOnBookDetail(value: Boolean) {
            viewModelScope.launch { store.setShowChapterDurationOnBookDetail(value) }
        }

        fun setAllowSeekFromNotification(value: Boolean) {
            viewModelScope.launch { store.setAllowSeekFromNotification(value) }
        }

        fun setThemeMode(value: ThemeMode) {
            viewModelScope.launch { store.setThemeMode(value) }
        }
    }
