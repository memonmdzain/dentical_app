package com.dentical.staff.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dentical.staff.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onNavigate: (String) -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🦷 Dentical Staff") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Dashboard") },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CalendarMonth, "Appointments") },
                    label = { Text("Schedule") },
                    selected = selectedTab == 1,
                    onClick = { onNavigate(Screen.Appointments.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.People, "Patients") },
                    label = { Text("Patients") },
                    selected = selectedTab == 2,
                    onClick = { onNavigate(Screen.Patients.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Receipt, "Billing") },
                    label = { Text("Billing") },
                    selected = selectedTab == 3,
                    onClick = { onNavigate(Screen.Billing.route) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Welcome back!", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Dashboard — coming soon",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
