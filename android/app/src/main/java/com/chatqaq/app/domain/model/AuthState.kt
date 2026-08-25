package com.chatqaq.app.domain.model

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Authenticated(val user: User) : AuthState
    data class Unauthenticated(val message: String? = null) : AuthState
    data class Error(val errorMessage: String) : AuthState
}
