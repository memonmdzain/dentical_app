package com.dentical.staff.ui.appointments

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dentical.staff.data.local.entities.AppointmentStatus
import com.dentical.staff.data.local.entities.AppointmentType
import com.dentical.staff.util.PhoneUtil
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppointmentCard(
    item: AppointmentWithDetails,
    onClick: () -> Unit,
    showDate: Boolean = false
) {
    val context = LocalContext.current
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateFormatter = SimpleDateFormat("EEEE, MMM d yyyy", Locale.getDefault())

    val phone = item.patient?.phone ?: item.patient?.guardianPhone
    val hasPhone = !phone.isNullOrBlank()

    fun dial() {
        phone?.let {
            val intent = Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:${PhoneUtil.formatForDialing(it)}"))
            context.startActivity(intent)
        }
    }

    fun openWhatsApp() {
        phone?.let {
            val intent = Intent(Intent.ACTION_VIEW,
                Uri.parse(PhoneUtil.whatsAppUrl(it)))
            context.startActivity(intent)
        }
    }

    Column {
        if (showDate) {
            Text(
                text = dateFormatter.format(Date(item.appointment.scheduledAt)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }

        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = timeFormatter.format(Date(item.appointment.scheduledAt)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(" · ${item.appointment.durationMinutes} min",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusBadge(item.appointment.status)
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = item.patient?.fullName ?: "Unknown Patient",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "ID: ${item.patient?.patientCode ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MedicalServices, null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = item.appointment.type.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (item.dentist != null) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text("Dr. ${item.dentist.fullName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                // Call & WhatsApp buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { if (hasPhone) dial() },
                        enabled = hasPhone,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Call", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { if (hasPhone) openWhatsApp() },
                        enabled = hasPhone,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Message, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("WhatsApp", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: AppointmentStatus) {
    val (text, color, bg) = when (status) {
        AppointmentStatus.SCHEDULED -> Triple("Scheduled",
            MaterialTheme.colorScheme.onSecondaryContainer,
            MaterialTheme.colorScheme.secondaryContainer)
        AppointmentStatus.CONFIRMED -> Triple("Confirmed",
            MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.primaryContainer)
        AppointmentStatus.IN_PROGRESS -> Triple("In Progress",
            MaterialTheme.colorScheme.onTertiaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer)
        AppointmentStatus.COMPLETED -> Triple("Completed",
            Color(0xFF1B5E20), Color(0xFFE8F5E9))
        AppointmentStatus.CANCELLED -> Triple("Cancelled",
            MaterialTheme.colorScheme.onErrorContainer,
            MaterialTheme.colorScheme.errorContainer)
        AppointmentStatus.NO_SHOW -> Triple("No Show",
            Color(0xFF4A148C), Color(0xFFF3E5F5))
    }
    Surface(shape = MaterialTheme.shapes.small, color = bg) {
        Text(text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}
