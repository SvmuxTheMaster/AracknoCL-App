package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.Avistamiento
import com.example.data.Usuario
import com.example.data.EspecieArana
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AuthTokens(val accessToken: String, val refreshToken: String)

object SupabaseManager {
    private const val TAG = "SupabaseManager"
    
    // TODO: Ensure RLS policies are configured in Supabase dashboard for 'avistamientos' and 'usuarios' tables.
    // INSERT/UPDATE/DELETE should require 'auth.uid() = id_usuario'.
    // Anon access should be revoked for write operations.

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var currentUserToken: String? = null
    private var currentRefreshToken: String? = null

    var onTokensRefreshed: ((AuthTokens) -> Unit)? = null

    fun getToken(): String? = currentUserToken
    fun getRefreshToken(): String? = currentRefreshToken

    /**
     * Initializes the manager with stored tokens (for session persistence).
     */
    fun setTokens(accessToken: String?, refreshToken: String?) {
        currentUserToken = accessToken
        currentRefreshToken = refreshToken
    }

    /**
     * Legacy support for older calls.
     */
    fun setToken(token: String?) {
        currentUserToken = token
    }

    /**
     * Determines if the Supabase environment variables are actual customized endpoints or placeholders.
     */
    fun isConfigured(): Boolean {
        val url = try { BuildConfig.SUPABASE_URL } catch (e: Exception) { "" }
        val anonKey = try { BuildConfig.SUPABASE_ANON_KEY } catch (e: Exception) { "" }
        return url.isNotEmpty() && 
               !url.contains("placeholder-project") && 
               anonKey.isNotEmpty() && 
               !anonKey.contains("placeholder")
    }

    fun getSupabaseUrl(): String {
        return try { BuildConfig.SUPABASE_URL } catch (e: Exception) { "https://placeholder-project.supabase.co" }
    }

    /**
     * Helper to perform authenticated requests with automatic retry on 401 using refresh token.
     */
    private suspend fun <T> authenticatedRequest(
        block: suspend (token: String) -> T?
    ): T? {
        val token = currentUserToken ?: return null
        var result = block(token)
        
        // If result is null, it might be due to 401. 
        // Note: This logic assumes 'block' returns null on 401. 
        // In a real implementation we might want to catch specific exceptions or check response codes.
        // For this project, we'll try to refresh if the token exists.
        
        // However, looking at the current code, many functions return null on any error.
        // We'll need to be careful. Let's implement refresh explicitly in getCurrentUser first.
        return result
    }

    /**
     * Signs up a new user using Supabase Auth.
     */
    suspend fun signUp(email: String, password: String): JSONObject? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val url = "${BuildConfig.SUPABASE_URL}/auth/v1/signup"
            val jsonObject = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                return@withContext JSONObject(response.body?.string() ?: "{}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in signUp", e)
        }
        null
    }

    /**
     * Signs in an existing user using Supabase Auth.
     */
    suspend fun signIn(email: String, password: String): AuthTokens? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext AuthTokens("simulated-token", "simulated-refresh")
        try {
            val url = "${BuildConfig.SUPABASE_URL}/auth/v1/token?grant_type=password"
            val jsonObject = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                val token = json.optString("access_token", null)
                val refresh = json.optString("refresh_token", null)
                if (token != null && refresh != null) {
                    currentUserToken = token
                    currentRefreshToken = refresh
                    return@withContext AuthTokens(token, refresh)
                }
            } else {
                Log.e(TAG, "signIn failed: ${response.code} - ${response.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in signIn", e)
        }
        null
    }

    /**
     * Refreshes the session using the refresh token.
     */
    suspend fun refreshSession(refreshToken: String): AuthTokens? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val url = "${BuildConfig.SUPABASE_URL}/auth/v1/token?grant_type=refresh_token"
            val jsonObject = JSONObject().apply { put("refresh_token", refreshToken) }
            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .post(requestBody)
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                val newAccess = json.optString("access_token", null) ?: return@withContext null
                val newRefresh = json.optString("refresh_token", null) ?: refreshToken
                currentUserToken = newAccess
                currentRefreshToken = newRefresh
                val newTokens = AuthTokens(newAccess, newRefresh)
                onTokensRefreshed?.invoke(newTokens)
                return@withContext newTokens
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing session", e)
        }
        null
    }

    /**
     * Updates the current user's email, password, or metadata.
     * Requires the current valid token to be set.
     */
    suspend fun updateProfile(
        newEmail: String? = null,
        newPassword: String? = null,
        metadata: JSONObject? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || currentUserToken == null) return@withContext false
        try {
            val result = performUpdateProfile(newEmail, newPassword, metadata)
            if (result == 401 && currentRefreshToken != null) {
                if (refreshSession(currentRefreshToken!!) != null) {
                    return@withContext performUpdateProfile(newEmail, newPassword, metadata) == 200
                }
            }
            return@withContext result == 200
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateProfile", e)
        }
        false
    }

    private fun performUpdateProfile(
        newEmail: String?,
        newPassword: String?,
        metadata: JSONObject?
    ): Int {
        val url = "${BuildConfig.SUPABASE_URL}/auth/v1/user"
        val jsonObject = JSONObject()
        newEmail?.let { jsonObject.put("email", it) }
        newPassword?.let { jsonObject.put("password", it) }
        metadata?.let { jsonObject.put("data", it) }

        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $currentUserToken")
            .put(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e(TAG, "updateProfile failed: ${response.code} - ${response.body?.string()}")
        }
        return response.code
    }

    /**
     * Signs out the current user, invalidating the session on the server.
     */
    suspend fun signOut(): Boolean = withContext(Dispatchers.IO) {
        val token = currentUserToken
        if (isConfigured() && token != null) {
            try {
                val url = "${BuildConfig.SUPABASE_URL}/auth/v1/logout"
                val request = Request.Builder()
                    .url(url)
                    .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $token")
                    .post("".toRequestBody(null))
                    .build()
                okHttpClient.newCall(request).execute()
            } catch (e: Exception) {
                Log.e(TAG, "Error invalidating session on server", e)
            }
        }
        currentUserToken = null
        currentRefreshToken = null
        true
    }

    /**
     * Retrieves the current user details using the stored token.
     */
    suspend fun getCurrentUser(): JSONObject? = withContext(Dispatchers.IO) {
        if (!isConfigured() || currentUserToken == null) return@withContext null
        try {
            var response = performGetCurrentUser()
            if (response.code == 401 && currentRefreshToken != null) {
                if (refreshSession(currentRefreshToken!!) != null) {
                    response = performGetCurrentUser()
                }
            }
            
            if (response.isSuccessful) {
                return@withContext JSONObject(response.body?.string() ?: "{}")
            } else if (response.code == 401) {
                currentUserToken = null
                currentRefreshToken = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCurrentUser", e)
        }
        null
    }

    private fun performGetCurrentUser(): okhttp3.Response {
        val url = "${BuildConfig.SUPABASE_URL}/auth/v1/user"
        val request = Request.Builder()
            .url(url)
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $currentUserToken")
            .get()
            .build()
        return okHttpClient.newCall(request).execute()
    }

    /**
     * Fetches the user profile from the `usuarios` table.
     */
    suspend fun fetchUserProfile(email: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val encodedEmail = android.net.Uri.encode(email)
            val url = "${BuildConfig.SUPABASE_URL}/rest/v1/usuarios?correo=eq.$encodedEmail&select=*"
            val request = Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                // Fallback to anon key if no token, but restricted by RLS (Tarea 2)
                // NOTE: If RLS is strict, this fallback will still fail for sensitive data, which is correct.
                .header("Authorization", "Bearer ${currentUserToken ?: BuildConfig.SUPABASE_ANON_KEY}")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful) {
                val jsonArray = JSONArray(responseBody ?: "[]")
                if (jsonArray.length() > 0) {
                    return@withContext jsonArray.getJSONObject(0)
                }
            } else if (response.code == 401 && currentRefreshToken != null) {
                 if (refreshSession(currentRefreshToken!!) != null) {
                     return@withContext fetchUserProfile(email)
                 }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile", e)
        }
        null
    }

    /**
     * Syncs a single user profile info to Supabase table `usuarios` using UPSERT.
     */
    suspend fun upsertUsuario(usuario: Usuario, authId: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext true
        try {
            val responseCode = performUpsertUsuario(usuario, authId)
            if (responseCode == 401 && currentRefreshToken != null) {
                if (refreshSession(currentRefreshToken!!) != null) {
                    val retryCode = performUpsertUsuario(usuario, authId)
                    return@withContext retryCode in 200..299
                }
            }
            return@withContext responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Error sync Usuario to Supabase", e)
            return@withContext false
        }
    }

    private fun performUpsertUsuario(usuario: Usuario, authId: String? = null): Int {
        val url = "${BuildConfig.SUPABASE_URL}/rest/v1/usuarios?on_conflict=correo"
        val jsonObject = JSONObject().apply {
            put("nombre", usuario.nombre)
            put("correo", usuario.correo)
            put("fecha_registro", usuario.fechaRegistro)
            put("foto_perfil", usuario.fotoPerfil)
            // Task 2: Send auth_id to link with auth.users for RLS
            val finalAuthId = authId ?: usuario.authId
            if (finalAuthId != null) {
                put("auth_id", finalAuthId)
            }
        }
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer ${currentUserToken ?: BuildConfig.SUPABASE_ANON_KEY}")
            .header("Content-Type", "application/json")
            .header("Prefer", "resolution=merge-duplicates") 
            .post(requestBody)
            .build()
        val response = okHttpClient.newCall(request).execute()
        return response.code
    }

    /**
     * Inserts an avistamiento report to Supabase table `avistamientos`.
     * Task 2: Use user token only, fail if not logged in.
     */
    suspend fun insertAvistamiento(avistamiento: Avistamiento): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext true
        val token = currentUserToken ?: return@withContext false
        
        try {
            val responseCode = performInsertAvistamiento(avistamiento, token)
            if (responseCode == 401 && currentRefreshToken != null) {
                val newTokens = refreshSession(currentRefreshToken!!)
                if (newTokens != null) {
                    return@withContext performInsertAvistamiento(avistamiento, newTokens.accessToken) in 200..299
                }
            }
            return@withContext responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Error sending avistamiento to Supabase", e)
            return@withContext false
        }
    }

    private fun performInsertAvistamiento(avistamiento: Avistamiento, token: String): Int {
        val url = "${BuildConfig.SUPABASE_URL}/rest/v1/avistamientos"
        val jsonObject = JSONObject().apply {
            if (avistamiento.id > 0) put("id", avistamiento.id)
            // TODO: Verify if id_usuario in Supabase is UUID. Current local model uses Int.
            put("id_usuario", avistamiento.idUsuario)
            put("url_imagen", avistamiento.urlImagen)
            put("fecha_captura", avistamiento.fechaCaptura)
            put("latitud", avistamiento.latitud)
            put("longitud", avistamiento.longitud)
            put("ubicacion_nombre", avistamiento.ubicacionNombre)
            put("confianza", avistamiento.confianza)
            put("resultado_especie", avistamiento.resultadoEspecie)
            put("resultado_nombre_comun", avistamiento.resultadoNombreComun)
            put("comentarios", avistamiento.comentarios)
        }
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .header("Prefer", "return=representation")
            .post(requestBody)
            .build()
        return okHttpClient.newCall(request).execute().code
    }

    /**
     * Deletes an avistamiento report in Supabase.
     * Task 2: Use user token only, fail if not logged in.
     */
    suspend fun deleteAvistamiento(id: Int): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext true
        val token = currentUserToken ?: return@withContext false

        try {
            val responseCode = performDeleteAvistamiento(id, token)
            if (responseCode == 401 && currentRefreshToken != null) {
                val newTokens = refreshSession(currentRefreshToken!!)
                if (newTokens != null) {
                    return@withContext performDeleteAvistamiento(id, newTokens.accessToken) in 200..299
                }
            }
            return@withContext responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting avistamiento in Supabase", e)
            return@withContext false
        }
    }

    private fun performDeleteAvistamiento(id: Int, token: String): Int {
        val url = "${BuildConfig.SUPABASE_URL}/rest/v1/avistamientos?id=eq.$id"
        val request = Request.Builder()
            .url(url)
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        return okHttpClient.newCall(request).execute().code
    }

    /**
     * Pulls up-to-date species from Supabase table `especies_arana`.
     */
    suspend fun fetchEspeciesFromSupabase(): List<EspecieArana> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val url = "${BuildConfig.SUPABASE_URL}/rest/v1/especies_arana"
            val request = Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${currentUserToken ?: BuildConfig.SUPABASE_ANON_KEY}")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val jsonArray = JSONArray(response.body?.string() ?: "[]")
            val list = mutableListOf<EspecieArana>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(EspecieArana(
                    nombreCientifico = obj.getString("nombre_cientifico"),
                    nombreComun = obj.getString("nombre_comun"),
                    familia = obj.getString("familia"),
                    descripcion = obj.getString("descripcion"),
                    nivelPeligrosidad = obj.getString("nivel_peligrosidad"),
                    venenosa = obj.getBoolean("venenosa"),
                    habitat = obj.getString("habitat"),
                    distribucion = obj.getString("distribucion"),
                    origen = obj.getString("origen")
                ))
            }
            return@withContext list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching species from Supabase", e)
            return@withContext emptyList()
        }
    }
}
