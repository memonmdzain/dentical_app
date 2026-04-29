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
import java.io.File

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read crash trace from file (primary) or SharedPreferences (fallback).
        // Do NOT delete it here — only delete when the user dismisses the dialog,
        // so a second crash on this launch cannot erase the evidence.
        val crashFile = File(filesDir, "crash.txt")
        val prefs = getSharedPreferences("crash", Context.MODE_PRIVATE)
        val savedCrash: String? = when {
            crashFile.exists() -> crashFile.readText().takeIf { it.isNotBlank() }
            else -> prefs.getString("trace", null)
        }

        enableEdgeToEdge()
        setContent {
            DenticalTheme {
                var crash by remember { mutableStateOf(savedCrash) }

                if (crash != null) {
                    // Show ONLY the dialog — do not render NavHost while a crash is
                    // pending, in case NavHost itself is what keeps crashing.
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Crash Report (share with developer)") },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(crash ?: "", style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                crashFile.delete()
                                prefs.edit().remove("trace").apply()
                                crash = null
                            }) { Text("Dismiss") }
                        }
                    )
                } else {
                    DenticalNavHost()
                }
            }
        }
    }
}
