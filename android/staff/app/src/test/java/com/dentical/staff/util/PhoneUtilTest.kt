package com.dentical.staff.util

import org.junit.Assert.*
import org.junit.Test

class PhoneUtilTest {

    // ── formatForDialing() ────────────────────────────────────────────────────

    @Test
    fun `number starting with + is returned as-is`() {
        assertEquals("+919876543210", PhoneUtil.formatForDialing("+919876543210"))
    }

    @Test
    fun `number starting with 00 replaces 00 with +`() {
        assertEquals("+919876543210", PhoneUtil.formatForDialing("00919876543210"))
    }

    @Test
    fun `number starting with 0 replaces 0 with +91`() {
        assertEquals("+919876543210", PhoneUtil.formatForDialing("09876543210"))
    }

    @Test
    fun `plain 10 digit number gets +91 prepended`() {
        assertEquals("+919876543210", PhoneUtil.formatForDialing("9876543210"))
    }

    @Test
    fun `leading and trailing spaces are trimmed before formatting`() {
        assertEquals("+919876543210", PhoneUtil.formatForDialing("  9876543210  "))
    }

    // ── formatForWhatsApp() ───────────────────────────────────────────────────

    @Test
    fun `formatForWhatsApp strips + spaces and dashes`() {
        assertEquals("919876543210", PhoneUtil.formatForWhatsApp("+91 98765-43210"))
    }

    @Test
    fun `formatForWhatsApp on plain number formats then strips`() {
        assertEquals("919876543210", PhoneUtil.formatForWhatsApp("9876543210"))
    }

    // ── whatsAppUrl() ─────────────────────────────────────────────────────────

    @Test
    fun `whatsAppUrl returns correct wa dot me URL`() {
        assertEquals("https://wa.me/919876543210", PhoneUtil.whatsAppUrl("9876543210"))
    }

    @Test
    fun `whatsAppUrl contains no + spaces or dashes`() {
        val url = PhoneUtil.whatsAppUrl("+91 98765-43210")
        assertFalse(url.contains("+"))
        assertFalse(url.contains(" "))
        assertFalse(url.contains("-"))
    }
}
