package com.cuetone.cuetonesync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cuetone.cuetonesync.ui.navigation.AppNavigation
import com.cuetone.cuetonesync.ui.theme.CueToneSyncTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CueToneSyncTheme {
                AppNavigation()
            }
        }
    }
}