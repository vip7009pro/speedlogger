package com.hnpage.speedloggernew.global

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val appStateManager: AppStateManager
): ViewModel()
{
    val appState: StateFlow<AppState> = appStateManager.state
    fun login(userName: String) {
        appStateManager.setLoading(isLoading = true)
        appStateManager.login(userName)
    }
    fun logout() {
        appStateManager.logout()
    }

}