package com.chatqaq.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chatqaq.app.core.network.TokenManager
import com.chatqaq.app.data.repository.AuthRepository
import com.chatqaq.app.domain.model.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val userId = try { tokenManager.getUserId() } catch (e: Exception) { null }
                val accessToken = try { tokenManager.getAccessToken() } catch (e: Exception) { null }

                if (userId != null && accessToken != null) {
                    val cachedUser = try { authRepository.getCachedUser(userId) } catch (e: Exception) { null }
                    if (cachedUser != null) {
                        _authState.value = AuthState.Authenticated(cachedUser)
                    }

                    val result = runCatching { authRepository.fetchCurrentUser() }.getOrNull()
                    if (result != null && result.isSuccess) {
                        _authState.value = AuthState.Authenticated(result.getOrThrow())
                    } else if (cachedUser == null) {
                        _authState.value = AuthState.Unauthenticated()
                    }
                } else {
                    _authState.value = AuthState.Unauthenticated()
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated()
            }
        }
    }

    fun login(login: String, password: String) {
        if (login.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please enter both login and password")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(login.trim(), password)
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.localizedMessage ?: "Login failed")
                }
            )
        }
    }

    fun loginWithGoogle(idToken: String) {
        if (idToken.isBlank()) return

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.loginWithGoogle(idToken)
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.localizedMessage ?: "Google Sign-In failed")
                }
            )
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String
    ) {
        if (firstName.isBlank() || lastName.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all required fields (First name, Last name, Username, Email, Password)")
            return
        }

        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.register(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                username = username.trim(),
                email = email.trim(),
                password = password
            )
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.localizedMessage ?: "Registration failed")
                }
            )
        }
    }

    fun updateProfile(displayName: String?, status: String?, bio: String?) {
        viewModelScope.launch {
            val result = authRepository.updateProfile(
                displayName = displayName,
                status = status,
                bio = bio
            )
            result.onSuccess { updatedUser ->
                _authState.value = AuthState.Authenticated(updatedUser)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.logout()
            _authState.value = AuthState.Unauthenticated()
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated()
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val tokenManager: TokenManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository, tokenManager) as T
        }
    }
}
