package com.dentical.staff.util

import java.security.MessageDigest

object PasswordUtil {
    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(password: String, hash: String): Boolean {
        return hash(password) == hash
    }
}
