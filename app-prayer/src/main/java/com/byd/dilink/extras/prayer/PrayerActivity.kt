package com.byd.dilink.extras.prayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.byd.dilink.extras.prayer.navigation.PrayerNavHost
import com.byd.dilink.extras.ui.theme.DiLinkExtrasTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiLinkExtrasTheme {
                PrayerNavHost()
            }
        }
    }
}
