package com.byd.dilink.extras.fuelcost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.byd.dilink.extras.fuelcost.navigation.FuelCostNavHost
import com.byd.dilink.extras.ui.theme.DiLinkBackground
import com.byd.dilink.extras.ui.theme.DiLinkExtrasTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FuelCostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DiLinkExtrasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DiLinkBackground
                ) {
                    FuelCostNavHost()
                }
            }
        }
    }
}
