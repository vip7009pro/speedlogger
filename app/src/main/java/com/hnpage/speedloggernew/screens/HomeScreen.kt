package com.hnpage.speedloggernew.screens

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hnpage.speedloggernew.global.AppViewModel
import com.hnpage.speedloggernew.global.LocalNavHostController

@Composable
fun HomeScreen(viewModel: AppViewModel = hiltViewModel())  {
    val appState by viewModel.appState.collectAsState()
    val navController: NavHostController = LocalNavHostController.current
    viewModel.login("Nguyen Van Hung")
    NavHost(
        navController = navController,
        startDestination = "screen1"
    ) {
        composable("screen1") { Screen1() }
        composable("screen2") { Screen2() }
        composable("screen3") { Screen3() }
    }

}

@Composable
fun Screen1() {
    val navController = LocalNavHostController.current

    Button(onClick = { navController.navigate("screen2") }) {
        Text("Go to Screen 2")
    }
}

@Composable
fun Screen2() {
    val navController = LocalNavHostController.current

    Button(onClick = { navController.navigate("screen3") }) {
        Text("Go to Screen 3")
    }
}

@Composable
fun Screen3() {
    val navController = LocalNavHostController.current

    Button(onClick = { navController.navigate("screen1") }) {
        Text("Go back to Screen 1")
    }
}
