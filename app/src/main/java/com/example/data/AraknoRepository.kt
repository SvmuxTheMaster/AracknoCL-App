package com.example.data

import com.example.network.SupabaseManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import android.util.Log

/**
 * Single source of truth for all data in the Arakno app.
 * Follows the Repository Pattern to abstract the data sources (Room cache + Supabase backend).
 */
class AraknoRepository(private val dao: AraknoDao) {

    // --- Observable Data Streams (Room Cache as Single Source of Truth for the UI) ---
    val usuario: Flow<Usuario?> = dao.getUsuario()
    val allEspecies: Flow<List<EspecieArana>> = dao.getAllEspecies()
    val allAvistamientos: Flow<List<Avistamiento>> = dao.getAllAvistamientos()

    /**
     * Synchronizes the species list from Supabase to the local Room database.
     * This ensures the app has up-to-date data and supports offline viewing.
     */
    suspend fun refreshSpeciesCache(): Result<Unit> {
        return try {
            val remoteSpecies = SupabaseManager.fetchEspeciesFromSupabase()
            if (remoteSpecies.isNotEmpty()) {
                // For a clean sync, we could clear and re-insert, or just use REPLACE strategy
                // dao.deleteAllEspecies() 
                dao.insertEspecies(remoteSpecies)
                Log.d("Repository", "Species cache refreshed with ${remoteSpecies.size} items.")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Repository", "Failed to refresh species cache", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches and caches the user profile from Supabase.
     */
    suspend fun fetchAndCacheUserProfile(email: String): Usuario? {
        return try {
            val profileJson = SupabaseManager.fetchUserProfile(email)
            if (profileJson != null) {
                val name = profileJson.optString("nombre")
                val foto = profileJson.optString("foto_perfil")
                
                // Get existing local user to avoid overwriting fields if remote is empty
                val existing = dao.getUsuario().firstOrNull()
                
                val user = Usuario(
                    id = 1,
                    nombre = if (name.isNotEmpty()) name else (existing?.nombre ?: email.substringBefore("@")),
                    correo = email,
                    fotoPerfil = if (foto.isNotEmpty()) foto else (existing?.fotoPerfil ?: "")
                )
                dao.insertUsuario(user)
                user
            } else {
                // If remote profile not found, don't overwrite local data with empties
                dao.getUsuario().firstOrNull()
            }
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching user profile", e)
            null
        }
    }

    suspend fun getEspecieByCientifico(nombreCientifico: String): EspecieArana? {
        return dao.getEspecieByCientifico(nombreCientifico)
    }

    suspend fun insertUsuario(usuario: Usuario) {
        // Always ensure we use ID 1 for the current active user in local cache
        val userToCache = if (usuario.id != 1) usuario.copy(id = 1) else usuario
        dao.insertUsuario(userToCache)
        
        try {
            SupabaseManager.upsertUsuario(userToCache)
        } catch (e: Exception) {
            Log.e("Repository", "Failed to sync user to Supabase", e)
        }
    }

    suspend fun insertAvistamiento(avistamiento: Avistamiento) {
        dao.insertAvistamiento(avistamiento)
        try {
            SupabaseManager.insertAvistamiento(avistamiento)
        } catch (e: Exception) {
            Log.e("Repository", "Failed to sync avistamiento to Supabase", e)
        }
    }

    suspend fun deleteAvistamientoById(id: Int) {
        dao.deleteAvistamientoById(id)
        try {
            SupabaseManager.deleteAvistamiento(id)
        } catch (e: Exception) {
            Log.e("Repository", "Failed to delete avistamiento from Supabase", e)
        }
    }

    // --- Authentication ---

    suspend fun checkSession(): Usuario? {
        val userJson = SupabaseManager.getCurrentUser()
        if (userJson != null) {
            val email = userJson.optString("email")
            if (email.isNotEmpty()) {
                // Try to get full profile from the custom table
                return fetchAndCacheUserProfile(email)
            }
        }
        return null
    }

    suspend fun signIn(email: String, pass: String): String? {
        return SupabaseManager.signIn(email, pass)
    }

    suspend fun signUp(email: String, pass: String): org.json.JSONObject? {
        return SupabaseManager.signUp(email, pass)
    }

    fun signOut() {
        SupabaseManager.signOut()
    }

    suspend fun clearLocalUser() {
        dao.clearUsuario()
    }
}
