package com.dentical.staff.data.remote

import android.util.Log
import com.dentical.staff.di.ApplicationScope
import com.dentical.staff.util.NetworkMonitor
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseSyncHelper @Inject constructor(
    val supabase: SupabaseClient,
    private val networkMonitor: NetworkMonitor,
    @ApplicationScope val scope: CoroutineScope
) {
    fun fireAndForget(block: suspend () -> Unit) {
        if (!networkMonitor.isConnected) return
        scope.launch {
            runCatching(block)
                .onFailure { Log.e("SupabaseSync", "Supabase sync failed", it) }
        }
    }

    fun delete(table: String, id: Long) = fireAndForget {
        supabase.from(table).delete { eq("id", id) }
    }
}
