package com.example

import android.app.Application
import android.content.Context
import com.example.data.AraknoDatabase
import com.example.data.AraknoRepository
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
        
        // Restore session
        val prefs = getSharedPreferences("arakno_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("supabase_token", null)
        SupabaseManager.setToken(token)

        applicationScope.launch {
            repository.refreshSpeciesCache()
            
            // We don't clear the token here anymore to avoid losing session on network errors.
            // Validation is handled by AraknoViewModel.
        }
    }
}
