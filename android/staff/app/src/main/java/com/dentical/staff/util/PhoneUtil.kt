package com.dentical.staff.util

object PhoneUtil {
    private const val DEFAULT_COUNTRY_CODE = "+91"

    fun formatForDialing(phone: String): String {
        val cleaned = phone.trim()
        return when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("00") -> "+" + cleaned.removePrefix("00")
            cleaned.startsWith("0") -> DEFAULT_COUNTRY_CODE + cleaned.removePrefix("0")
            else -> DEFAULT_COUNTRY_CODE + cleaned
        }
    }

    fun formatForWhatsApp(phone: String): String {
        val formatted = formatForDialing(phone)
        // Remove + and any spaces/dashes for WhatsApp URL
        return formatted.replace("+", "").replace(" ", "").replace("-", "")
    }

    fun whatsAppUrl(phone: String): String =
        "https://wa.me/${formatForWhatsApp(phone)}"
}
