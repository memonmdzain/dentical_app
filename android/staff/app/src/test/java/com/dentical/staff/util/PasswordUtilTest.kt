package com.dentical.staff.util

import org.junit.Assert.*
import org.junit.Test

class PasswordUtilTest {

    // ── hash() ────────────────────────────────────────────────────────────────

    @Test
    fun `hash returns 64 character lowercase hex string`() {
        val result = PasswordUtil.hash("password123")
        assertEquals(64, result.length)
        assertTrue("Hash should only contain hex characters", result.matches(Regex("[0-9a-f]+")))
    }

    @Test
    fun `hash is deterministic - same input returns same output`() {
        val input = "mySecret"
        assertEquals(PasswordUtil.hash(input), PasswordUtil.hash(input))
    }

    @Test
    fun `hash of empty string returns valid 64 char hex`() {
        val result = PasswordUtil.hash("")
        assertEquals(64, result.length)
        assertTrue(result.matches(Regex("[0-9a-f]+")))
    }

    // ── verify() ─────────────────────────────────────────────────────────────

    @Test
    fun `verify returns true when password matches its own hash`() {
        val password = "clinic@2024"
        val hash = PasswordUtil.hash(password)
        assertTrue(PasswordUtil.verify(password, hash))
    }

    @Test
    fun `verify returns false for wrong password`() {
        val hash = PasswordUtil.hash("correctPassword")
        assertFalse(PasswordUtil.verify("wrongPassword", hash))
    }

    @Test
    fun `verify is case sensitive`() {
        val hash = PasswordUtil.hash("admin")
        assertFalse(PasswordUtil.verify("Admin", hash))
        assertFalse(PasswordUtil.verify("ADMIN", hash))
    }
}
