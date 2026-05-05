package com.dentical.staff.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_USER_ID = longPreferencesKey("logged_in_user_id")
private const val NO_SESSION = -1L

@Singleton
class SessionManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val currentUserId: Flow<Long?> = dataStore.data.map { prefs ->
        val id = prefs[KEY_USER_ID] ?: NO_SESSION
        if (id == NO_SESSION) null else id
    }

    suspend fun setSession(userId: Long) {
        dataStore.edit { it[KEY_USER_ID] = userId }
    }

    suspend fun clearSession() {
        dataStore.edit { it[KEY_USER_ID] = NO_SESSION }
    }
}
