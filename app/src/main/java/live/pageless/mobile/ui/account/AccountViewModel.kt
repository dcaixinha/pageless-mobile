package live.pageless.mobile.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import live.pageless.mobile.data.repository.AuthRepository
import javax.inject.Inject

data class AccountUiState(
    val host: String = "",
    val username: String = "",
    val serverVersion: String? = null,
)

@HiltViewModel
class AccountViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val serverVersion = MutableStateFlow<String?>(null)

        val state: StateFlow<AccountUiState> =
            combine(authRepository.serverUrl, authRepository.email, serverVersion) { host, email, version ->
                AccountUiState(host = host, username = email ?: "", serverVersion = version)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState())

        init {
            viewModelScope.launch {
                authRepository.refreshCurrentUser().onSuccess { serverVersion.value = it.serverVersion }
            }
        }

        fun logout(onDone: () -> Unit) {
            viewModelScope.launch {
                authRepository.logout()
                onDone()
            }
        }
    }
