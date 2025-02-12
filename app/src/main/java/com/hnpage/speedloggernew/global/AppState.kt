package com.hnpage.speedloggernew.global

data class AppState(
    val isLoggedIn: Boolean = false,
    val userName: String? = null,
    val isLoading: Boolean = false
)