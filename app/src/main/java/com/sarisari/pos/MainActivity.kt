package com.sarisari.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sarisari.pos.ui.navigation.SariSariNavigation
import com.sarisari.pos.ui.theme.SariSariTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SariSariApp
        setContent {
            SariSariTheme {
                SariSariNavigation(app.container)
            }
        }
    }
}
