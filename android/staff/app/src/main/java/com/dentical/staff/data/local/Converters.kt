package com.dentical.staff.data.local

import androidx.room.TypeConverter
import com.dentical.staff.data.local.entities.AppointmentStatus
import com.dentical.staff.data.local.entities.AppointmentType
import com.dentical.staff.data.local.entities.InvoiceStatus
import com.dentical.staff.data.local.entities.TreatmentStatus
import com.dentical.staff.data.local.entities.UserRole

class Converters {
    @TypeConverter fun fromUserRole(v: UserRole): String = v.name
    @TypeConverter fun toUserRole(v: String): UserRole = UserRole.valueOf(v)

    @TypeConverter fun fromAppointmentStatus(v: AppointmentStatus): String = v.name
    @TypeConverter fun toAppointmentStatus(v: String): AppointmentStatus = AppointmentStatus.valueOf(v)

    @TypeConverter fun fromAppointmentType(v: AppointmentType): String = v.name
    @TypeConverter fun toAppointmentType(v: String): AppointmentType = AppointmentType.valueOf(v)

    @TypeConverter fun fromInvoiceStatus(v: InvoiceStatus): String = v.name
    @TypeConverter fun toInvoiceStatus(v: String): InvoiceStatus = InvoiceStatus.valueOf(v)

    @TypeConverter fun fromTreatmentStatus(v: TreatmentStatus): String = v.name
    @TypeConverter fun toTreatmentStatus(v: String): TreatmentStatus = TreatmentStatus.valueOf(v)
}
