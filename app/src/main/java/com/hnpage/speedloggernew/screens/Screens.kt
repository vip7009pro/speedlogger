package com.hnpage.speedloggernew.screens


import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hnpage.speedloggernew.global.AppViewModel

@Composable
fun AppScreen(viewModel: AppViewModel = hiltViewModel()) {
    val appState by viewModel.appState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (appState.isLoading) {
            Text(text = "Loading...")
        } else if (appState.isLoggedIn) {
            Text(text = "Welcome, ${appState.userName}")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.logout() }) {
                Text(text = "Logout")
            }
        } else {
            Text(text = "Please log in")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.login("John Doe") }) {
                Text(text = "Login")
            }
        }
    }
}
