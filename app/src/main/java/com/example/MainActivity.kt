package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.InstagramViewModel
import com.example.ui.components.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge layout for drawing beneath the status and navigation bars
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                // Initialize the unified ViewModel managing Room database and simulation loops
                val instaViewModel: InstagramViewModel = viewModel()
                
                // Mount the primary application coordinator
                MainScreen(
                    viewModel = instaViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
