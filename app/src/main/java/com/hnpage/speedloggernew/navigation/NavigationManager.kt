package com.hnpage.speedloggernew.navigation

import androidx.navigation.NavOptions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationManager @Inject constructor() {
    private val _navigationEvent = MutableSharedFlow<NavigationCommand>(extraBufferCapacity = 1)
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun navigateTo(command: NavigationCommand) {
        _navigationEvent.tryEmit(command)
    }
}

data class NavigationCommand(
    val destination: String,
    val navOptions: NavOptions? = null,
    val singleTop: Boolean = false
)