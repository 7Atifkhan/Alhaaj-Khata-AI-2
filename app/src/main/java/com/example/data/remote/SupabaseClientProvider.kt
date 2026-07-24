package com.example.data.remote

import com.example.BuildConfig

object SupabaseClientProvider {

    private fun getBuildConfigField(fieldName: String): String? {
        return try {
            val field = BuildConfig::class.java.getField(fieldName)
            field.get(null) as? String
        } catch (e: Exception) {
            null
        }
    }

    val SUPABASE_URL: String
        get() {
            val fromBuildConfig = getBuildConfigField("SUPABASE_URL")
                ?: getBuildConfigField("VITE_SUPABASE_URL")
            if (!fromBuildConfig.isNullOrBlank()) {
                return fromBuildConfig
            }
            val fromEnv = System.getenv("SUPABASE_URL")
                ?: System.getenv("VITE_SUPABASE_URL")
            if (!fromEnv.isNullOrBlank()) {
                return fromEnv
            }
            return "https://your-project.supabase.co"
        }

    val SUPABASE_ANON_KEY: String
        get() {
            val fromBuildConfig = getBuildConfigField("SUPABASE_ANON_KEY")
                ?: getBuildConfigField("VITE_SUPABASE_ANON_KEY")
            if (!fromBuildConfig.isNullOrBlank()) {
                return fromBuildConfig
            }
            val fromEnv = System.getenv("SUPABASE_ANON_KEY")
                ?: System.getenv("VITE_SUPABASE_ANON_KEY")
            if (!fromEnv.isNullOrBlank()) {
                return fromEnv
            }
            return "your-anon-key"
        }

    fun isConfigured(): Boolean {
        val url = SUPABASE_URL
        return url.startsWith("https://") && !url.contains("your-project") && !url.contains("your-supabase-project")
    }
}
