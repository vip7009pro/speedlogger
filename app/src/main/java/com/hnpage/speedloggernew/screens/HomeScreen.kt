package com.hnpage.speedloggernew.screens

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
import com.hnpage.speedloggernew.navigation.NavRoutes
import com.hnpage.speedloggernew.navigation.NavigationViewModel
import kotlinx.coroutines.flow.collectLatest

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
        composable(NavRoutes.DETAIL) { Screen2() }
        composable(NavRoutes.SETTINGS) { Screen3() }
    }

}

@Composable
fun Screen1(viewModel: NavigationViewModel = hiltViewModel()) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = { viewModel.navigateTo(NavRoutes.DETAIL) }) {
            Text("Go to screen2")
        }
        Button(onClick = { viewModel.navigateTo(NavRoutes.SETTINGS) }) {
            Text("Go to screen3")
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
