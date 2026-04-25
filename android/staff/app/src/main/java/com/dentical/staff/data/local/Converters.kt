package com.dentical.staff.data.local

import androidx.room.TypeConverter
import com.dentical.staff.data.local.entities.AppointmentStatus
import com.dentical.staff.data.local.entities.InvoiceStatus
import com.dentical.staff.data.local.entities.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(role: String): UserRole = UserRole.valueOf(role)

    @TypeConverter
    fun fromAppointmentStatus(status: AppointmentStatus): String = status.name

    @TypeConverter
    fun toAppointmentStatus(status: String): AppointmentStatus =
        AppointmentStatus.valueOf(status)

    @TypeConverter
    fun fromInvoiceStatus(status: InvoiceStatus): String = status.name

    @TypeConverter
    fun toInvoiceStatus(status: String): InvoiceStatus = InvoiceStatus.valueOf(status)
}
