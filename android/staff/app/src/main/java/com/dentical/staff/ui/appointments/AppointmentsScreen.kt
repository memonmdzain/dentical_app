package com.dentical.staff.ui.appointments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    onAddAppointment: () -> Unit,
    onAppointmentClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: AppointmentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appointments") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Toggle view mode
                    IconButton(onClick = viewModel::toggleViewMode) {
                        Icon(
                            if (uiState.viewMode == AppointmentViewMode.LIST)
                                Icons.Default.CalendarMonth
                            else Icons.Default.ViewList,
                            contentDescription = "Toggle View",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAppointment,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add Appointment",
                    tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.viewMode == AppointmentViewMode.CALENDAR) {
                // Calendar view type tabs
                TabRow(selectedTabIndex = uiState.calendarViewType.ordinal) {
                    CalendarViewType.values().forEach { type ->
                        Tab(
                            selected = uiState.calendarViewType == type,
                            onClick = { viewModel.setCalendarViewType(type) },
                            text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                CalendarView(
                    uiState = uiState,
                    onDateSelected = viewModel::setSelectedDate,
                    onAppointmentClick = onAppointmentClick
                )
            } else {
                ListView(
                    uiState = uiState,
                    onAppointmentClick = onAppointmentClick
                )
            }
        }
    }
}

@Composable
fun ListView(
    uiState: AppointmentsUiState,
    onAppointmentClick: (Long) -> Unit
) {
    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.appointments.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CalendarMonth, null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
                Text("No upcoming appointments",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline)
            }
        }
        return
    }

    // Group appointments by date
    val grouped = uiState.appointments.groupBy { item ->
        val cal = Calendar.getInstance().apply { timeInMillis = item.appointment.scheduledAt }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (_, items) ->
            itemsIndexed(items) { index, item ->
                AppointmentCard(
                    item = item,
                    onClick = { onAppointmentClick(item.appointment.id) },
                    showDate = index == 0
                )
            }
        }
    }
}

@Composable
fun CalendarView(
    uiState: AppointmentsUiState,
    onDateSelected: (Long) -> Unit,
    onAppointmentClick: (Long) -> Unit
) {
    val dateFormatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
        // Selected date header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                onDateSelected(uiState.selectedDate - 86_400_000L)
            }) {
                Icon(Icons.Default.ChevronLeft, "Previous")
            }
            Text(
                text = dateFormatter.format(Date(uiState.selectedDate)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = {
                onDateSelected(uiState.selectedDate + 86_400_000L)
            }) {
                Icon(Icons.Default.ChevronRight, "Next")
            }
        }

        HorizontalDivider()

        if (uiState.appointments.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No appointments for this day",
                    color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.appointments) { _, item ->
                    AppointmentCard(
                        item = item,
                        onClick = { onAppointmentClick(item.appointment.id) },
                        showDate = false
                    )
                }
            }
        }
    }
}
