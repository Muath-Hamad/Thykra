package com.jameeli.thykra.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(onNavigateToProfile: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thykra") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Text("\u2699")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Text("Welcome to Thykra!")
            Text("Your shared albums will appear here.")
        }
    }
}
