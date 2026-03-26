package com.byd.dilink.extras.tireguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.byd.dilink.extras.tireguard.navigation.TireGuardNavHost
import com.byd.dilink.extras.ui.theme.DiLinkExtrasTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TireGuardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiLinkExtrasTheme {
                TireGuardNavHost()
            }
        }
    }
}
