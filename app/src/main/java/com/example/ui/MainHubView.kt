package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Avistamiento
import com.example.data.EspecieArana
import com.example.data.Usuario
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHubView(
    viewModel: AraknoViewModel,
    onNavigateToScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect states
    val usuario by viewModel.usuario.collectAsStateWithLifecycle()
    val filteredEspecies by viewModel.filteredEspecies.collectAsStateWithLifecycle()
    val allEspecies by viewModel.allEspecies.collectAsStateWithLifecycle()
    val avistamientos by viewModel.avistamientos.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedEspecie by viewModel.selectedEspecie.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("home") } // "home", "guide", "journal", "profile", "help"
    val context = androidx.compose.ui.platform.LocalContext.current

    // Scaffold with clean cohesive dark theme
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AraknoBottomNavigation(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen router
            Crossfade(
                targetState = activeTab,
                animationSpec = tween(300),
                label = "screen_routing"
            ) { tab ->
                when (tab) {
                    "home" -> HomeScreen(
                        usuario = usuario,
                        sightingCount = avistamientos.size,
                        onScanClick = onNavigateToScan,
                        onQuickFaqClick = { activeTab = "help" },
                        onNavigateToGuide = { activeTab = "guide" },
                        onNavigateToJournal = { activeTab = "journal" }
                    )
                    "guide" -> GuiaInformativaScreen(
                        especies = filteredEspecies,
                        selectedFilter = selectedFilter,
                        searchQuery = searchQuery,
                        onFilterSelected = { viewModel.selectFilter(it) },
                        onSearchChanged = { viewModel.updateSearchQuery(it) },
                        onEspecieClick = { viewModel.selectEspecie(it) }
                    )
                    "journal" -> MiDiarioScreen(
                        avistamientos = avistamientos,
                        onDeleteSighting = { viewModel.deleteSighting(it) },
                        onEspecieClick = { scientificName ->
                            val match = allEspecies.firstOrNull { it.nombreCientifico == scientificName }
                            if (match != null) {
                                viewModel.selectEspecie(match)
                            } else {
                                android.widget.Toast.makeText(context, "Información técnica no disponible para esta especie", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    "profile" -> {
                        val coroutineScope = rememberCoroutineScope()
                        PerfilScreen(
                            usuario = usuario,
                            sightingCount = avistamientos.size,
                            onSaveProfile = { nom, cor, foto, pass, newPass -> 
                                coroutineScope.launch {
                                    val result = viewModel.updateProfile(nom, cor, foto, newPass)
                                    if (result.isSuccess) {
                                        android.widget.Toast.makeText(context, "Perfil actualizado", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onLogout = { viewModel.logout(context) }
                        )
                    }
                    "help" -> AyudaFaqScreen()
                }
            }

            // Analysis Modal overlay if we currently have an on-going scans
            if (analysisState !is AnalysisState.Idle) {
                AnalysisOverlayDialog(
                    state = analysisState,
                    onDismiss = { viewModel.resetAnalysis() },
                    onViewFicha = { especie ->
                        viewModel.selectEspecie(especie)
                        viewModel.resetAnalysis()
                        activeTab = "guide"
                    }
                )
            }

            // Ficha Técnica Immersive Detail Dialog overlay
            if (selectedEspecie != null) {
                FichaTecnicaDialog(
                    especie = selectedEspecie!!,
                    onDismiss = { viewModel.selectEspecie(null) }
                )
            }
        }
    }
}

// Custom Styled navigation following premium dark vibe
@Composable
fun AraknoBottomNavigation(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        NavigationBarItem(
            selected = activeTab == "home",
            onClick = { onTabSelected("home") },
            label = { Text("Inicio", style = MaterialTheme.typography.labelSmall) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.testTag("nav_home")
        )
        NavigationBarItem(
            selected = activeTab == "guide",
            onClick = { onTabSelected("guide") },
            label = { Text("Guía", style = MaterialTheme.typography.labelSmall) },
            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.testTag("nav_guide")
        )
        NavigationBarItem(
            selected = activeTab == "journal",
            onClick = { onTabSelected("journal") },
            label = { Text("Diario", style = MaterialTheme.typography.labelSmall) },
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.testTag("nav_journal")
        )
        NavigationBarItem(
            selected = activeTab == "profile",
            onClick = { onTabSelected("profile") },
            label = { Text("Perfil", style = MaterialTheme.typography.labelSmall) },
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.testTag("nav_profile")
        )
        NavigationBarItem(
            selected = activeTab == "help",
            onClick = { onTabSelected("help") },
            label = { Text("Ayuda", style = MaterialTheme.typography.labelSmall) },
            icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.testTag("nav_help")
        )
    }
}

@Composable
fun SupabaseStatusIndicatorVertical() {
    val isConfigured = remember { com.example.network.SupabaseManager.isConfigured() }
    val supabaseUrl = remember { com.example.network.SupabaseManager.getSupabaseUrl() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isConfigured) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (isConfigured) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (isConfigured) "Soporte Supabase: Activo" else "Soporte Supabase: Integrado (Modo Local)",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isConfigured) supabaseUrl else "Detección guardará datos localmente y replicará a Supabase al configurar claves.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
