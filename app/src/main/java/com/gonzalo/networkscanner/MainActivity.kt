package com.gonzalo.networkscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gonzalo.networkscanner.ui.theme.NetworkScannerTheme
import com.gonzalo.networkscanner.ui.views.NetworkScannerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetworkScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NetworkScannerScreen()
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NetworkScannerTheme {
        NetworkScannerScreen()
    }
}