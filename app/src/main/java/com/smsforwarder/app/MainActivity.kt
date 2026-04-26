package com.smsforwarder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smsforwarder.app.ui.AppRoot
import com.smsforwarder.app.ui.theme.SmsForwarderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmsForwarderTheme {
                AppRoot()
            }
        }
    }
}
