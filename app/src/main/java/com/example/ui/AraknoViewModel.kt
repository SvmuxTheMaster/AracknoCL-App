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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
        // Initial check for session
        checkInitialSession()
    }

    private fun checkInitialSession() {
        viewModelScope.launch {
            // Check if we have a non-guest user in DB
            repository.usuario.collect { user ->
                if (user != null && user.correo != "explorador@arakno.cl") {
                    _authState.value = AuthState.Authenticated
                } else if (user != null && user.correo == "explorador@arakno.cl") {
                    // It's the guest user, if we have a token, we might need to fetch profile
                    // but for simplicity, we treat "explorador@arakno.cl" as non-authenticated
                    _authState.value = AuthState.Idle
                }
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

    // Combined Flow for Spiders list filtering
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

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
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
                // Save session
                context.getSharedPreferences("arakno_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("supabase_token", token)
                    .apply()

                // Fetch the real profile from Supabase
                val profile = repository.getUserProfile(email)
                android.util.Log.d("AraknoVM", "Profile fetched: $profile")
                
                // Try to get name from profile, fallback to email prefix if not found
                val name = profile?.optString("nombre")?.takeIf { it.isNotBlank() } 
                    ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

                val phone = profile?.optString("celular") ?: ""
                
                // Use id = 1 to overwrite the seeded guest user
                val current = Usuario(id = 1, nombre = name, correo = email, celular = phone)
                android.util.Log.d("AraknoVM", "Inserting user to DB: $current")
                repository.insertUsuario(current)
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Error("Credenciales inválidas")
            }
        }
    }

    fun register(email: String, pass: String, nombre: String, context: Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.signUp(email, pass)
            if (result != null) {
                // For Supabase, sign up doesn't always return a token immediately if email confirmation is on
                // but let's assume we can try to login or get token here.
                // If it was successful, we might need a separate login call or handle response.
                
                val newUsuario = Usuario(id = 1, nombre = nombre, correo = email)
                repository.insertUsuario(newUsuario)
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Error("Error al registrar usuario")
            }
        }
    }

    fun logout(context: Context) {
        repository.signOut()
        _authState.value = AuthState.Idle
        // Clear session
        context.getSharedPreferences("arakno_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("supabase_token")
            .apply()

        // Reset local user to default guest
        viewModelScope.launch {
            repository.insertUsuario(
                Usuario(
                    id = 1,
                    nombre = "Explorador de Chile",
                    correo = "explorador@arakno.cl",
                    celular = "+56 9 8765 4321"
                )
            )
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    // --- Profile actions ---
    fun updateProfile(nombre: String, correo: String, celular: String) {
        viewModelScope.launch {
            val current = usuario.value ?: Usuario(id = 1, nombre = nombre, correo = correo, celular = celular)
            repository.insertUsuario(current.copy(nombre = nombre, correo = correo, celular = celular))
        }
    }

    // --- Gemini Analysis trigger ---
    fun analyzeCapturedImage(bitmap: Bitmap, locationName: String = "Santiago, RM") {
        _analysisState.value = AnalysisState.Loading
        viewModelScope.launch {
            try {
                val result = GeminiManager.analyzeSpiderImage(bitmap)
                _analysisState.value = AnalysisState.Success(result, bitmap)
                
                // If spider was successfully validated and matched, let's automatically save to dairy "Mi Diario"!
                if (result.spiderFound && result.especie != null) {
                    val fallbackComun = result.especie.nombreComun
                    val fallbackCientifico = result.especie.nombreCientifico
                    
                    // Add avistamiento entry in local Room DB
                    repository.insertAvistamiento(
                        Avistamiento(
                            urlImagen = "", // In actual app, we can save file to internal storage. Here, we'll cache indicator or keep empty.
                            ubicacionNombre = locationName,
                            confianza = result.confianza,
                            resultadoEspecie = fallbackCientifico,
                            resultadoNombreComun = fallbackComun,
                            comentarios = "Identificación inteligente con IA Arakno CL. (${result.especie.familia})"
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

    // --- Simple Factory class ---
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
