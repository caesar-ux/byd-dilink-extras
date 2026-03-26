package com.byd.dilink.extras.hazard

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.byd.dilink.extras.hazard.navigation.HazardNavHost
import com.byd.dilink.extras.ui.theme.DiLinkBackground
import com.byd.dilink.extras.ui.theme.DiLinkExtrasTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HazardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            DiLinkExtrasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DiLinkBackground
                ) {
                    HazardNavHost()
                }
            }
        }
    }
}
