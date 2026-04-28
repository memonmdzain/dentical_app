package com.dentical.staff

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.dentical.staff.ui.theme.DenticalTheme
import com.dentical.staff.ui.navigation.DenticalNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("crash", Context.MODE_PRIVATE)
        val savedCrash = prefs.getString("trace", null)
        if (savedCrash != null) {
            prefs.edit().remove("trace").apply()
        }

        enableEdgeToEdge()
        setContent {
            DenticalTheme {
                var crash by remember { mutableStateOf(savedCrash) }

                DenticalNavHost()

                crash?.let { trace ->
                    AlertDialog(
                        onDismissRequest = { crash = null },
                        title = { Text("Crash Report") },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(trace, style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { crash = null }) { Text("Dismiss") }
                        }
                    )
                }
            }
        }
    }
}
