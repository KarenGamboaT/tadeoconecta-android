package co.edu.utadeo.tadeoconecta.core.network

import co.edu.utadeo.tadeoconecta.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlin.time.Duration.Companion.seconds

object SupabaseProvider {
    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.startsWith("https://") && BuildConfig.SUPABASE_KEY.isNotBlank()

    val client: SupabaseClient by lazy {
        require(isConfigured) {
            "Supabase no está configurado. Define SUPABASE_URL y SUPABASE_KEY en local.properties."
        }
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            // El valor por defecto (10 s) se queda corto para generar el plan
            // de mejora; las revisiones de texto igual fallan rápido porque
            // sus Edge Functions cortan a los 8 s.
            requestTimeout = 30.seconds
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Functions)
        }
    }
}
