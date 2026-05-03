package com.dentical.staff.di

import android.util.Log
import com.dentical.staff.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        if (url.isBlank()) Log.e("SupabaseModule", "SUPABASE_URL is empty — cloud sync disabled. Check GitHub secrets.")
        if (key.isBlank()) Log.e("SupabaseModule", "SUPABASE_ANON_KEY is empty — cloud sync disabled. Check GitHub secrets.")
        return createSupabaseClient(
            supabaseUrl = url.ifBlank { "https://placeholder.supabase.co" },
            supabaseKey = key.ifBlank { "placeholder" }
        ) { install(Postgrest) }
    }

    @ApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
