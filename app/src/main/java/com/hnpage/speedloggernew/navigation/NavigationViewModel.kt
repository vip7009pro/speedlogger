package com.hnpage.speedloggernew.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val navigationManager: NavigationManager
) : ViewModel() {
    val navigationEvent = navigationManager.navigationEvent

    fun navigateTo(destination: String, singleTop: Boolean = true) {
        navigationManager.navigateTo(
            NavigationCommand(
                destination = destination,
                singleTop = singleTop
            )
        )
    }
}