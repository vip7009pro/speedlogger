package com.hnpage.speedloggernew.screens

import CameraScreen
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hnpage.speedloggernew.MainViewModel
import com.hnpage.speedloggernew.db.LocationViewModel
import com.hnpage.speedloggernew.navigation.NavRoutes
import com.hnpage.speedloggernew.navigation.NavigationViewModel
import com.hnpage.speedloggernew.services.CarAppService
import com.hnpage.speedloggernew.services.LocationForegroundService
import kotlinx.coroutines.flow.collectLatest

class HS : ComponentActivity()  {
    private val mainViewModel: MainViewModel by viewModels()
    val locationViewModel: LocationViewModel by viewModels()



    private fun stopService() {
        val intent = Intent(this, LocationForegroundService::class.java)
        stopService(intent) // Dừng service
        Log.d("MainActivity", "Service stop requested")
    }

    private fun startService() {
        mainViewModel.speedOffset.value?.let { LocationForegroundService.startService(this, it) }
    }

    @Composable
    fun HomeScreen(viewModel: NavigationViewModel = hiltViewModel())  {
        val navController = rememberNavController()
        val navigationManager = hiltViewModel<NavigationViewModel>()

        LaunchedEffect(Unit) {
            navigationManager.navigationEvent.collectLatest { command ->
                // Sử dụng navigate với String route và NavOptions
                navController.navigate(command.destination, command.navOptions)
            }
        }
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME
        ) {
            composable(NavRoutes.HOME) { Screen1() }
            composable(NavRoutes.DETAIL) {

                SpeedLogScreens().MainScreen(viewModel = mainViewModel,
                    lctvm = locationViewModel,
                    onStopService = { stopService() },
                    onStartService = { startService() })
            }
            composable(NavRoutes.SETTINGS) { CameraScreen() }
        }

    }

    @Composable
    fun Screen1(viewModel: NavigationViewModel = hiltViewModel()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Button(onClick = { viewModel.navigateTo(NavRoutes.DETAIL) }) {
                Text("Go to speedlogger")
            }
            Button(onClick = { viewModel.navigateTo(NavRoutes.SETTINGS) }) {
                Text("Go to camera")
            }
        }
    }

    @Composable
    fun Screen2(viewModel: NavigationViewModel = hiltViewModel()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Button(onClick = { viewModel.navigateTo(NavRoutes.HOME) }) {
                Text("Back to Home")
            }
        }
    }

    @Composable
    fun Screen3(viewModel: NavigationViewModel = hiltViewModel()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Button(onClick = { viewModel.navigateTo(NavRoutes.HOME) }) {
                Text("Back to Home")
            }
        }
    }

}


