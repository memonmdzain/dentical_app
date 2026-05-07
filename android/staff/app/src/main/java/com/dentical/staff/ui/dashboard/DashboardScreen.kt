package com.dentical.staff.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dentical.staff.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    onProfile: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val canSync by viewModel.canSync.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🦷 Dentical Staff") },
                actions = {
                    IconButton(onClick = viewModel::onSyncClick, enabled = canSync) {
                        if (isSyncing)
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        else
                            Icon(Icons.Default.Sync, "Sync",
                                tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onProfile) {
                        Icon(Icons.Default.AccountCircle, "Profile", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
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
                    selected = true,
                    onClick = {}
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CalendarMonth, "Appointments") },
                    label = { Text("Schedule") },
                    selected = false,
                    onClick = { onNavigate(Screen.Appointments.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.People, "Patients") },
                    label = { Text("Patients") },
                    selected = false,
                    onClick = { onNavigate(Screen.Patients.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Receipt, "Billing") },
                    label = { Text("Billing") },
                    selected = false,
                    onClick = { onNavigate(Screen.Billing.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, "Settings") },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { onNavigate(Screen.Settings.route) }
                )
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Welcome back!", style = MaterialTheme.typography.headlineMedium)

                StatCard(
                    label = "Ongoing Treatments",
                    value = uiState.ongoingTreatmentCount.toString(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = { onNavigate(Screen.PatientListDashboard.createRoute("ongoing")) }
                )

                StatCard(
                    label = "Today's Collections",
                    value = "₹${"%.2f".format(uiState.todaysCollections)}",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )

                StatCard(
                    label = "Total Outstanding",
                    value = "₹${"%.2f".format(uiState.totalOutstanding)}",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    onClick = { onNavigate(Screen.PatientListDashboard.createRoute("outstanding")) }
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onClick != null) {
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}
