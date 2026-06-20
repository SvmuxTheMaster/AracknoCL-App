package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefs {
    private const val PREFS_NAME = "arakno_secure_prefs"
    private const val KEY_TOKEN = "supabase_token"
    private const val KEY_REFRESH_TOKEN = "supabase_refresh_token"

    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(context: Context, accessToken: String?, refreshToken: String? = null) {
        getPrefs(context).edit().apply {
            if (accessToken != null) putString(KEY_TOKEN, accessToken) else remove(KEY_TOKEN)
            if (refreshToken != null) putString(KEY_REFRESH_TOKEN, refreshToken) else remove(KEY_REFRESH_TOKEN)
            apply()
        }
    }

    fun getAccessToken(context: Context): String? {
        return getPrefs(context).getString(KEY_TOKEN, null)
    }

    fun getRefreshToken(context: Context): String? {
        return getPrefs(context).getString(KEY_REFRESH_TOKEN, null)
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
