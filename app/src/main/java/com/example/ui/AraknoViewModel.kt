package com.example.ui

import android.graphics.Bitmap
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AraknoRepository
import com.example.data.Avistamiento
import com.example.data.EspecieArana
import com.example.data.Usuario
import com.example.network.GeminiManager
import com.example.network.SupabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed interface AnalysisState {
    object Idle : AnalysisState
    object Loading : AnalysisState
    data class Success(val result: GeminiManager.AnalysisResult, val image: Bitmap?) : AnalysisState
    data class Error(val message: String) : AnalysisState
}

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    object Authenticated : AuthState
    data class Error(val message: String) : AuthState
}

class AraknoViewModel(private val repository: AraknoRepository) : ViewModel() {

    init {
        // Initial sync of species from Supabase
        syncData()
        // Initial check for session from stored token and cached user
        checkInitialSession()
    }

    private fun syncData() {
        viewModelScope.launch {
            repository.refreshSpeciesCache()
        }
    }

    private fun checkInitialSession() {
        viewModelScope.launch {
            // Use firstOrNull to perform a one-time initial check and avoid state flickering
            val user = repository.usuario.firstOrNull()
            
            // If there is a cached user with a valid email, consider authenticated
            if (user != null && user.correo.contains("@") && user.correo != "explorador@arakno.cl") {
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Idle
            }
        }
    }

    // --- Core states from DB ---
    val usuario: StateFlow<Usuario?> = repository.usuario
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allEspecies: StateFlow<List<EspecieArana>> = repository.allEspecies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val avistamientos: StateFlow<List<Avistamiento>> = repository.allAvistamientos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search & Filters ---
    private val _selectedFilter = MutableStateFlow("Todos")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredEspecies: StateFlow<List<EspecieArana>> = combine(
        allEspecies, _selectedFilter, _searchQuery
    ) { especies, filter, query ->
        especies.filter { esp ->
            val matchesFilter = if (filter == "Todos") true else esp.origen.equals(filter, ignoreCase = true)
            val matchesQuery = if (query.isEmpty()) true else {
                esp.nombreComun.contains(query, ignoreCase = true) ||
                        esp.nombreCientifico.contains(query, ignoreCase = true) ||
                        esp.familia.contains(query, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active states for Analysis & Scanner ---
    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _selectedEspecie = MutableStateFlow<EspecieArana?>(null)
    val selectedEspecie: StateFlow<EspecieArana?> = _selectedEspecie.asStateFlow()

    fun selectFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectEspecie(especie: EspecieArana?) {
        _selectedEspecie.value = especie
    }

    fun resetAnalysis() {
        _analysisState.value = AnalysisState.Idle
    }

    fun login(email: String, pass: String, context: Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val token = repository.signIn(email, pass)
            if (token != null) {
                // Set token in manager immediately
                SupabaseManager.setToken(token)
                
                // Persistent session
                context.getSharedPreferences("arakno_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("supabase_token", token)
                    .apply()

                // Fetch real profile from Supabase and cache it in Room
                repository.fetchAndCacheUserProfile(email)
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Error("Credenciales inválidas o error de red")
            }
        }
    }

    fun register(email: String, pass: String, nombre: String, context: Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.signUp(email, pass)
            if (result != null) {
                // If sign up was successful, Supabase might not return token immediately depending on config
                // But we can try to sign in or just set user info
                val token = result.optString("access_token", "")
                if (token.isNotEmpty()) {
                    SupabaseManager.setToken(token)
                    context.getSharedPreferences("arakno_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("supabase_token", token)
                        .apply()
                }

                // Insert profile to both Supabase and Room
                val newUsuario = Usuario(id = 1, nombre = nombre, correo = email)
                repository.insertUsuario(newUsuario)
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Error("Error al registrar usuario en Supabase")
            }
        }
    }

    fun logout(context: Context) {
        repository.signOut()
        _authState.value = AuthState.Idle
        context.getSharedPreferences("arakno_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("supabase_token")
            .apply()

        // Clear local user data entirely (no more Explorador de Chile)
        viewModelScope.launch {
            repository.clearLocalUser()
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    suspend fun updateProfile(
        nombre: String,
        correo: String,
        fotoPerfil: String,
        currentPass: String,
        newPass: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Logic to update password if provided
            if (!newPass.isNullOrBlank()) {
                val authSuccess = SupabaseManager.updateProfile(newPassword = newPass)
                if (!authSuccess) {
                    return@withContext Result.failure(Exception("Error al actualizar la contraseña en el servidor. Reintente más tarde."))
                }
            }

            // Update details in local and remote database
            val current = usuario.value ?: Usuario(id = 1, nombre = nombre, correo = correo)
            val updatedUser = current.copy(
                nombre = nombre,
                correo = correo,
                fotoPerfil = fotoPerfil
            )
            repository.insertUsuario(updatedUser)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun analyzeCapturedImage(bitmap: Bitmap, locationName: String = "Santiago, RM") {
        _analysisState.value = AnalysisState.Loading
        viewModelScope.launch {
            try {
                val result = GeminiManager.analyzeSpiderImage(bitmap)
                _analysisState.value = AnalysisState.Success(result, bitmap)
                
                if (result.spiderFound && result.especie != null) {
                    repository.insertAvistamiento(
                        Avistamiento(
                            urlImagen = "",
                            ubicacionNombre = locationName,
                            confianza = result.confianza,
                            resultadoEspecie = result.especie.nombreCientifico,
                            resultadoNombreComun = result.especie.nombreComun,
                            comentarios = "Identificación inteligente con IA Arakno CL."
                        )
                    )
                }
            } catch (e: Exception) {
                _analysisState.value = AnalysisState.Error(e.localizedMessage ?: "Fallo al procesar imagen")
            }
        }
    }

    fun deleteSighting(id: Int) {
        viewModelScope.launch {
            repository.deleteAvistamientoById(id)
        }
    }

    class Factory(private val repository: AraknoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AraknoViewModel::class.java)) {
                return AraknoViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
