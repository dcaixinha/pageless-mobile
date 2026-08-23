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

        /**
         * Fills the form with the public demo server's details.
         *
         * Deliberately fills rather than signs in: the point is to show what a
         * Pageless server address looks like, and to let someone read the
         * credentials before committing to them.
         */
        fun useDemoServer() =
            _state.update {
                it.copy(
                    serverUrl = DEMO_SERVER_URL,
                    email = DEMO_EMAIL,
                    password = DEMO_PASSWORD,
                    error = null,
                )
            }

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

        companion object {
            /**
             * Public demo server, offered from the sign-in screen so the app can
             * be tried without running a server first.
             *
             * These credentials are public by design — they are committed to a
             * public repository and handed out by a button. The account holds
             * nothing private, and anyone using it shares one library: progress,
             * bookmarks and history sync last-write-wins, so concurrent demo
             * users will see each other's positions move.
             */
            const val DEMO_SERVER_URL = "https://demo.pageless.live"
            const val DEMO_EMAIL = "demo@example.com"
            const val DEMO_PASSWORD = "demouser1234"
        }
    }
