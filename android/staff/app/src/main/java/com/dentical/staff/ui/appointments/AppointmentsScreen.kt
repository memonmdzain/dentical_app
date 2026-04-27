package com.dentical.staff.ui.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                TabRow(selectedTabIndex = uiState.calendarViewType.ordinal) {
                    CalendarViewType.values().forEach { type ->
                        Tab(
                            selected = uiState.calendarViewType == type,
                            onClick = { viewModel.setCalendarViewType(type) },
                            text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                when (uiState.calendarViewType) {
                    CalendarViewType.DAY -> DayView(
                        uiState = uiState,
                        onDateChange = viewModel::setSelectedDate,
                        onAppointmentClick = onAppointmentClick
                    )
                    CalendarViewType.WEEK -> WeekView(
                        uiState = uiState,
                        onDateSelected = viewModel::setSelectedDate,
                        onAppointmentClick = onAppointmentClick
                    )
                    CalendarViewType.MONTH -> MonthView(
                        uiState = uiState,
                        onDateSelected = viewModel::setSelectedDate,
                        onAppointmentClick = onAppointmentClick
                    )
                }
            } else {
                ListView(uiState = uiState, onAppointmentClick = onAppointmentClick)
            }
        }
    }
}

// ─── List View ───────────────────────────────────────────────────────────────

@Composable
fun ListView(uiState: AppointmentsUiState, onAppointmentClick: (Long) -> Unit) {
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

// ─── Day View ────────────────────────────────────────────────────────────────

@Composable
fun DayView(
    uiState: AppointmentsUiState,
    onDateChange: (Long) -> Unit,
    onAppointmentClick: (Long) -> Unit
) {
    val dateFormatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onDateChange(uiState.selectedDate - 86_400_000L) }) {
                Icon(Icons.Default.ChevronLeft, "Previous")
            }
            Text(dateFormatter.format(Date(uiState.selectedDate)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { onDateChange(uiState.selectedDate + 86_400_000L) }) {
                Icon(Icons.Default.ChevronRight, "Next")
            }
        }
        HorizontalDivider()
        if (uiState.appointments.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No appointments", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.appointments) { item ->
                    AppointmentCard(item = item,
                        onClick = { onAppointmentClick(item.appointment.id) },
                        showDate = false)
                }
            }
        }
    }
}

// ─── Week View ───────────────────────────────────────────────────────────────

@Composable
fun WeekView(
    uiState: AppointmentsUiState,
    onDateSelected: (Long) -> Unit,
    onAppointmentClick: (Long) -> Unit
) {
    val dayFormatter = SimpleDateFormat("EEE", Locale.getDefault())
    val dateFormatter = SimpleDateFormat("d", Locale.getDefault())
    val monthFormatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    // Get start of week containing selectedDate
    val weekStart = remember(uiState.selectedDate) {
        val cal = Calendar.getInstance().apply { timeInMillis = uiState.selectedDate }
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val weekDays = remember(weekStart) {
        (0..6).map { weekStart + it * 86_400_000L }
    }

    // Count appointments per day
    val countPerDay = remember(uiState.appointments, weekDays) {
        weekDays.associateWith { dayStart ->
            uiState.appointments.count { item ->
                val apptDay = Calendar.getInstance().apply {
                    timeInMillis = item.appointment.scheduledAt
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                apptDay == dayStart
            }
        }
    }

    val selectedDayAppointments = uiState.appointments.filter { item ->
        val apptDay = Calendar.getInstance().apply {
            timeInMillis = item.appointment.scheduledAt
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        apptDay == uiState.selectedDate
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Month + navigation
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onDateSelected(weekStart - 7 * 86_400_000L) }) {
                Icon(Icons.Default.ChevronLeft, "Previous Week")
            }
            Text(monthFormatter.format(Date(weekStart)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { onDateSelected(weekStart + 7 * 86_400_000L) }) {
                Icon(Icons.Default.ChevronRight, "Next Week")
            }
        }

        // 7 day strip
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weekDays.forEach { dayMillis ->
                val isSelected = dayMillis == uiState.selectedDate
                val count = countPerDay[dayMillis] ?: 0
                val isToday = dayMillis == todayStartMillis()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else if (isToday) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { onDateSelected(dayMillis) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayFormatter.format(Date(dayMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = dateFormatter.format(Date(dayMillis)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    if (count > 0) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.height(18.dp))
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

        // Appointments for selected day
        if (selectedDayAppointments.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No appointments for this day",
                    color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedDayAppointments) { item ->
                    AppointmentCard(item = item,
                        onClick = { onAppointmentClick(item.appointment.id) },
                        showDate = false)
                }
            }
        }
    }
}

// ─── Month View ──────────────────────────────────────────────────────────────

@Composable
fun MonthView(
    uiState: AppointmentsUiState,
    onDateSelected: (Long) -> Unit,
    onAppointmentClick: (Long) -> Unit
) {
    val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val cal = remember(uiState.selectedDate) {
        Calendar.getInstance().apply { timeInMillis = uiState.selectedDate }
    }

    val monthStart = remember(uiState.selectedDate) {
        Calendar.getInstance().apply {
            timeInMillis = uiState.selectedDate
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val daysInMonth = remember(uiState.selectedDate) {
        Calendar.getInstance().apply {
            timeInMillis = uiState.selectedDate
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val firstDayOfWeek = remember(monthStart) {
        Calendar.getInstance().apply { timeInMillis = monthStart }.get(Calendar.DAY_OF_WEEK) - 1
    }

    // Count appointments per day of month
    val countPerDay = remember(uiState.appointments) {
        val map = mutableMapOf<Int, Int>()
        uiState.appointments.forEach { item ->
            val c = Calendar.getInstance().apply { timeInMillis = item.appointment.scheduledAt }
            val cMonth = c.get(Calendar.MONTH)
            val cYear = c.get(Calendar.YEAR)
            val selCal = Calendar.getInstance().apply { timeInMillis = uiState.selectedDate }
            if (cMonth == selCal.get(Calendar.MONTH) && cYear == selCal.get(Calendar.YEAR)) {
                val day = c.get(Calendar.DAY_OF_MONTH)
                map[day] = (map[day] ?: 0) + 1
            }
        }
        map
    }

    val selectedDayAppointments = uiState.appointments.filter { item ->
        val apptDay = Calendar.getInstance().apply {
            timeInMillis = item.appointment.scheduledAt
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        apptDay == uiState.selectedDate
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val c = Calendar.getInstance().apply { timeInMillis = uiState.selectedDate }
                c.add(Calendar.MONTH, -1)
                onDateSelected(c.timeInMillis)
            }) { Icon(Icons.Default.ChevronLeft, "Previous Month") }
            Text(monthFormatter.format(Date(uiState.selectedDate)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            IconButton(onClick = {
                val c = Calendar.getInstance().apply { timeInMillis = uiState.selectedDate }
                c.add(Calendar.MONTH, 1)
                onDateSelected(c.timeInMillis)
            }) { Icon(Icons.Default.ChevronRight, "Next Month") }
        }

        // Day labels
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            dayLabels.forEach { label ->
                Text(label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(4.dp))

        // Calendar grid
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7

        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val day = cellIndex - firstDayOfWeek + 1
                        if (day < 1 || day > daysInMonth) {
                            Box(modifier = Modifier.weight(1f).height(44.dp))
                        } else {
                            val dayCal = Calendar.getInstance().apply {
                                timeInMillis = monthStart
                                set(Calendar.DAY_OF_MONTH, day)
                            }
                            val dayMillis = dayCal.timeInMillis
                            val isSelected = dayMillis == uiState.selectedDate
                            val isToday = dayMillis == todayStartMillis()
                            val count = countPerDay[day] ?: 0

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else if (isToday) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { onDateSelected(dayMillis) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (count > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = count.toString(),
                                                    fontSize = 8.sp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Appointments for selected day
        if (selectedDayAppointments.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No appointments for this day",
                    color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedDayAppointments) { item ->
                    AppointmentCard(item = item,
                        onClick = { onAppointmentClick(item.appointment.id) },
                        showDate = false)
                }
            }
        }
    }
}
