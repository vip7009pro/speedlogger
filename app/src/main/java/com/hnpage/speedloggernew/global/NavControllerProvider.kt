package com.hnpage.speedloggernew.global
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

val LocalNavHostController = staticCompositionLocalOf<NavHostController> {
    error("No NavController provided")
}
