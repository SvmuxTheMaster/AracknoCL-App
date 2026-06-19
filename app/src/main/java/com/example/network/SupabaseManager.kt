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

object SupabaseManager {
    private const val TAG = "SupabaseManager"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var currentUserToken: String? = null

    fun getToken(): String? = currentUserToken

    /**
     * Initializes the manager with a stored token (for session persistence).
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
    suspend fun signIn(email: String, password: String): String? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext "simulated-token"
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
                currentUserToken = token
                return@withContext token
            } else {
                Log.e(TAG, "signIn failed: ${response.code} - ${response.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in signIn", e)
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
            if (response.isSuccessful) {
                return@withContext true
            } else {
                Log.e(TAG, "updateProfile failed: ${response.code} - ${response.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateProfile", e)
        }
        false
    }

    fun signOut() {
        currentUserToken = null
    }

    /**
     * Retrieves the current user details using the stored token.
     */
    suspend fun getCurrentUser(): JSONObject? = withContext(Dispatchers.IO) {
        if (!isConfigured() || currentUserToken == null) return@withContext null
        try {
            val url = "${BuildConfig.SUPABASE_URL}/auth/v1/user"
            val request = Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $currentUserToken")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                return@withContext JSONObject(response.body?.string() ?: "{}")
            } else if (response.code == 401) {
                // Token expired or invalid
                currentUserToken = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCurrentUser", e)
        }
        null
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
                .header("Authorization", "Bearer ${currentUserToken ?: BuildConfig.SUPABASE_ANON_KEY}")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d(TAG, "fetchUserProfile response: ${response.code} - $responseBody")
            
            if (response.isSuccessful) {
                val jsonArray = JSONArray(responseBody ?: "[]")
                if (jsonArray.length() > 0) {
                    return@withContext jsonArray.getJSONObject(0)
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
    suspend fun upsertUsuario(usuario: Usuario): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            Log.i(TAG, "Supabase key is not configured. Simulating usuario insert locally.")
            return@withContext true
        }

        try {
            // Use on_conflict=correo to ensure we update the correct user based on email
            val url = "${BuildConfig.SUPABASE_URL}/rest/v1/usuarios?on_conflict=correo"
            val jsonObject = JSONObject().apply {
                // Do NOT send the local ID '1' to Supabase as it will conflict between different users
                put("nombre", usuario.nombre)
                put("correo", usuario.correo)
                put("fecha_registro", usuario.fechaRegistro)
                put("foto_perfil", usuario.fotoPerfil)
            }

            val mediaType = "application/json".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${currentUserToken ?: BuildConfig.SUPABASE_ANON_KEY}")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates") 
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            val success = response.isSuccessful
            Log.d(TAG, "upsertUsuario response: ${response.code} - $responseBody")
            return@withContext success
        } catch (e: Exception) {
            Log.e(TAG, "Error sync Usuario to Supabase", e)
            return@withContext false
        }
    }

    /**
     * Inserts an avistamiento report to Supabase table `avistamientos`.
     */
    suspend fun insertAvistamiento(avistamiento: Avistamiento): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            Log.i(TAG, "Supabase key is not configured. Simulating remote avistamiento creation.")
            return@withContext true
        }

        try {
            val url = "${BuildConfig.SUPABASE_URL}/rest/v1/avistamientos"
            val jsonObject = JSONObject().apply {
                // If id > 0, post it. Otherwise let Supabase generate DB serial/identity id
                if (avistamiento.id > 0) {
                    put("id", avistamiento.id)
                }
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

            val mediaType = "application/json".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val success = response.isSuccessful
            Log.d(TAG, "insertAvistamiento success: $success code: ${response.code}")
            return@withContext success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending avistamiento to Supabase", e)
            return@withContext false
        }
    }

    /**
     * Deletes an avistamiento report in Supabase.
     */
    suspend fun deleteAvistamiento(id: Int): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            Log.i(TAG, "Supabase key is not configured. Simulating delete locally.")
            return@withContext true
        }

        try {
            val url = "${BuildConfig.SUPABASE_URL}/rest/v1/avistamientos?id=eq.$id"
            val request = Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .delete()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val success = response.isSuccessful
            Log.d(TAG, "deleteAvistamiento success: $success code: ${response.code}")
            return@withContext success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting avistamiento in Supabase", e)
            return@withContext false
        }
    }

    /**
     * Pulls up-to-date species from Supabase table `especies_arana` if available,
     * providing a dynamic way to insert or match other species.
     */
    suspend fun fetchEspeciesFromSupabase(): List<EspecieArana> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext emptyList()
        }

        try {
            val url = "${BuildConfig.SUPABASE_URL}/rest/v1/especies_arana"
            val request = Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val jsonArray = JSONArray(response.body?.string() ?: "[]")
            val list = mutableListOf<EspecieArana>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    EspecieArana(
                        nombreCientifico = obj.getString("nombre_cientifico"),
                        nombreComun = obj.getString("nombre_comun"),
                        familia = obj.getString("familia"),
                        descripcion = obj.getString("descripcion"),
                        nivelPeligrosidad = obj.getString("nivel_peligrosidad"),
                        venenosa = obj.getBoolean("venenosa"),
                        habitat = obj.getString("habitat"),
                        distribucion = obj.getString("distribucion"),
                        origen = obj.getString("origen")
                    )
                )
            }
            return@withContext list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching species from Supabase", e)
            return@withContext emptyList()
        }
    }
}
