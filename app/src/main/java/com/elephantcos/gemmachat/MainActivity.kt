package com.elephantcos.gemmachat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.elephantcos.gemmachat.ui.navigation.NavGraph
import com.elephantcos.gemmachat.ui.theme.GemmaChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GemmaChatTheme {
                NavGraph()
            }
        }
    }
}
