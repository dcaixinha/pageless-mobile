package live.pageless.mobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.pageless.mobile.BuildConfig
import live.pageless.mobile.data.repository.AuthRepository
import javax.inject.Inject

data class LoginUiState(
    val serverUrl: String = BuildConfig.DEFAULT_SERVER_URL,
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(LoginUiState())
        val state: StateFlow<LoginUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                _state.update { it.copy(serverUrl = authRepository.serverUrl.first()) }
            }
        }

        fun onServerUrlChange(v: String) = _state.update { it.copy(serverUrl = v, error = null) }

        fun onEmailChange(v: String) = _state.update { it.copy(email = v, error = null) }

        fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null) }

        fun login(onSuccess: () -> Unit) {
            val s = _state.value
            if (s.serverUrl.isBlank() || s.email.isBlank() || s.password.isBlank()) {
                _state.update { it.copy(error = "All fields are required") }
                return
            }
            _state.update { it.copy(loading = true, error = null) }
            viewModelScope.launch {
                val result = authRepository.login(s.serverUrl, s.email, s.password)
                result.fold(
                    onSuccess = {
                        _state.update { it.copy(loading = false) }
                        onSuccess()
                    },
                    onFailure = { e ->
                        _state.update {
                            it.copy(loading = false, error = e.message ?: "Login failed")
                        }
                    },
                )
            }
        }
    }
