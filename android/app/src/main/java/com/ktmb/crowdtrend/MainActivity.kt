package com.ktmb.crowdtrend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ktmb.crowdtrend.navigation.KtmbNavHost
import com.ktmb.crowdtrend.ui.theme.KtmbTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KtmbTheme {
                KtmbNavHost()
            }
        }
    }
}
