package com.hnpage.speedloggernew.global

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class AppStateManager @Inject constructor() {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state

    fun login(userName: String) {
        _state.update {
            it.copy(isLoggedIn = true, userName = userName, isLoading = false)
        }
    }

    fun logout() {
        _state.update {
            it.copy(isLoggedIn = false, userName = null, isLoading = false)
        }
    }
    fun setLoading(isLoading: Boolean) {
        _state.update {
            it.copy(isLoading = isLoading)
        }
    }

}