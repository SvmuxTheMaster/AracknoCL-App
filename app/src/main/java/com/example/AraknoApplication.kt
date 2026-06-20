package com.example

import android.app.Application
import android.content.Context
import com.example.data.AraknoDatabase
import com.example.data.AraknoRepository
import com.example.data.SecurePrefs
import com.example.network.SupabaseManager
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AraknoApplication : Application() {
    val database by lazy { AraknoDatabase.getDatabase(this) }
    val repository by lazy { AraknoRepository(database.dao) }

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        
        // Restore session with migration to SecurePrefs
        val legacyPrefs = getSharedPreferences("arakno_prefs", Context.MODE_PRIVATE)
        val legacyToken = legacyPrefs.getString("supabase_token", null)
        
        if (legacyToken != null) {
            SecurePrefs.saveToken(this, legacyToken)
            legacyPrefs.edit { clear() }
        }

        val token = SecurePrefs.getAccessToken(this)
        val refreshToken = SecurePrefs.getRefreshToken(this)
        SupabaseManager.setTokens(token, refreshToken)
        
        SupabaseManager.onTokensRefreshed = { tokens ->
            SecurePrefs.saveToken(this, tokens.accessToken, tokens.refreshToken)
        }

        applicationScope.launch {
            repository.refreshSpeciesCache()
            
            // We don't clear the token here anymore to avoid losing session on network errors.
            // Validation is handled by AraknoViewModel.
        }
    }
}
