package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NaturePeople
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Avistamiento
import com.example.data.EspecieArana
import com.example.data.Usuario
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

// 1. HOME SCREEN (HU01 - Interfaz Base)
@Composable
fun HomeScreen(
    usuario: Usuario?,
    sightingCount: Int,
    onScanClick: () -> Unit,
    onQuickFaqClick: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToJournal: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hola, ${usuario?.nombre ?: ""}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Has registrado $sightingCount encuentro(s) de arañas.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NaturePeople,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Giant Scanning Central Trigger Button (HU01)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 140.dp, minHeight = 140.dp, maxWidth = 200.dp, maxHeight = 200.dp)
                        .aspectRatio(1f)
                        .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                        .padding(8.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            ),
                            CircleShape
                        )
                        .clickable { onScanClick() }
                        .testTag("home_scan_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Escanear ahora con IA",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ESCANEAR ARAÑA",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Apunta a la araña para identificarla",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        // Fast Access Navigation Cards Grid
        item {
            Text(
                text = "Accesos Rápidos",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Guide access
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 110.dp)
                        .clickable { onNavigateToGuide() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                "Guía de Especies",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Fichas técnicas y hábitats",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Journal access
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 110.dp)
                        .clickable { onNavigateToJournal() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                "Mi Diario",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Tus capturas guardadas",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Education prompt card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onQuickFaqClick() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¿Venenoso vs Peligroso?",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aprende la diferencia en la guía de ayuda",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// 2. SPIDER GUIDE SCREEN (Guía Informativa)
@Composable
fun GuiaInformativaScreen(
    especies: List<EspecieArana>,
    selectedFilter: String,
    searchQuery: String,
    onFilterSelected: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onEspecieClick: (EspecieArana) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Guía de Especies",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Arañas más comunes registradas en Chile y su nivel de riesgo.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Buscar por nombre, familia...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("search_bar"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Filter chips (Endémico, Nativo, Exótico)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("Todos", "Endémico", "Nativo", "Exótico")
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("filter_chip_$filter"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // List
        if (especies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No se encontraron especies", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(especies, key = { it.nombreCientifico }) { especie ->
                    SpiderListItem(especie = especie, onClick = { onEspecieClick(especie) })
                }
            }
        }
    }
}

@Composable
fun SpiderListItem(
    especie: EspecieArana,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("spider_item_${especie.nombreCientifico.replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stylized safety warning indicator icon
            val colorAlert = when (especie.nivelPeligrosidad) {
                "Extrema" -> MaterialTheme.colorScheme.error
                "Alta" -> MaterialTheme.colorScheme.secondary
                "Media" -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    .border(1.5.dp, colorAlert, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (especie.nivelPeligrosidad) {
                        "Extrema", "Alta" -> Icons.Default.Warning
                        else -> Icons.Default.Eco
                    },
                    contentDescription = null,
                    tint = colorAlert,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = especie.nombreComun,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = especie.nombreCientifico,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Origin Badge
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(especie.origen, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    // Danger level Badge
                    Box(
                        modifier = Modifier
                            .background(colorAlert.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Riesgo: ${especie.nivelPeligrosidad}",
                            color = colorAlert,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// 3. DIARIO SCREEN (Mi Diario)
@Composable
fun MiDiarioScreen(
    avistamientos: List<Avistamiento>,
    onDeleteSighting: (Int) -> Unit,
    onEspecieClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mi Diario de Arañas",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Historial de encuentros identificados en terreno.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (avistamientos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f), modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aún no tienes encuentros registrados",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Usa el scanner de cámara para registrar tu primera araña",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(avistamientos, key = { it.id }) { sighting ->
                    SightingItem(
                        avistamiento = sighting,
                        onDeleteClick = { onDeleteSighting(sighting.id) },
                        onLearnMoreClick = { onEspecieClick(sighting.resultadoEspecie) }
                    )
                }
            }
        }
    }
}

@Composable
fun SightingItem(
    avistamiento: Avistamiento,
    onDeleteClick: () -> Unit,
    onLearnMoreClick: () -> Unit
) {
    val dateString = remember(avistamiento.fechaCaptura) {
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        formatter.format(Date(avistamiento.fechaCaptura))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sighting_item_${avistamiento.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PinDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = avistamiento.ubicacionNombre,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp).testTag("delete_sighting_${avistamiento.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sighting header
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.NaturePeople, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = avistamiento.resultadoNombreComun,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = avistamiento.resultadoEspecie,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${(avistamiento.confianza * 100).toInt()}% Confianza",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateString,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (avistamiento.comentarios.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = avistamiento.comentarios,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Ver ficha científica completa →",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLearnMoreClick() }
            )
        }
    }
}

// 4. PERFIL SCREEN (Perfil)
@Composable
fun PerfilScreen(
    usuario: Usuario?,
    sightingCount: Int,
    onSaveProfile: (String, String, String, String, String?) -> Unit,
    onLogout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isEditing by remember { mutableStateOf(false) }

    var username by remember(usuario?.nombre) { mutableStateOf(usuario?.nombre ?: "") }
    var email by remember(usuario?.correo) { mutableStateOf(usuario?.correo ?: "") }
    var fotoPerfil by remember(usuario?.fotoPerfil) { mutableStateOf(usuario?.fotoPerfil ?: "") }
    
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val outputStream = ByteArrayOutputStream()
                    // Compress to reduce size for Base64 storage
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val byteArray = outputStream.toByteArray()
                    fotoPerfil = Base64.encodeToString(byteArray, Base64.DEFAULT)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Perfil del Explorador",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Profile Header / Avatar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clip(CircleShape)
                        .clickable(enabled = isEditing) {
                            photoLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (fotoPerfil.isNotEmpty()) {
                        val bitmap = try {
                            val decodedString = Base64.decode(fotoPerfil, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        } catch (e: Exception) {
                            null
                        }

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(60.dp))
                        }
                    } else {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(60.dp))
                    }

                    if (isEditing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(32.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = username.ifBlank { "Explorador" },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Miembro activo de Arakno CL",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = sightingCount.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Encuentros",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Chile",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "Territorio",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isEditing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Section 1: Basic Info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Información Básica", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Nombre de usuario") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo electrónico") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(24.dp))

                    // Section 2: Security
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Seguridad y Contraseña", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Contraseña actual (Requerida)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                "Cambiar Contraseña", 
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium, 
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Completa ambos campos solo si deseas cambiarla", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("Nueva contraseña") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = confirmNewPassword,
                                onValueChange = { confirmNewPassword = it },
                                label = { Text("Confirmar nueva contraseña") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            errorMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        isEditing = false
                        errorMessage = null
                        currentPassword = ""
                        newPassword = ""
                        confirmNewPassword = ""
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        if (currentPassword.isEmpty()) {
                            errorMessage = "Se requiere contraseña actual para guardar cambios"
                            return@Button
                        }
                        if (newPassword.isNotEmpty() && newPassword != confirmNewPassword) {
                            errorMessage = "Las nuevas contraseñas no coinciden"
                            return@Button
                        }

                        onSaveProfile(
                            username,
                            email,
                            fotoPerfil,
                            currentPassword,
                            newPassword.ifBlank { null }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1.2f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            ProfileField(label = "Nombre", valStr = username, icon = Icons.Default.Person)
            ProfileField(label = "Correo electrónico", valStr = email, icon = Icons.Default.Email)

            Button(
                onClick = { isEditing = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), contentColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("profile_edit_toggle")
            ) {
                Text("Editar Perfil", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión")
            }
        }
    }
}

@Composable
fun AuthScreen(
    state: AuthState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onReset: () -> Unit
) {
    var isRegistering by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    fun validateInputs(): Boolean {
        if (email.isBlank() || password.isBlank()) {
            localError = "Email y contraseña son obligatorios"
            return false
        }
        if (isRegistering && nombre.isBlank()) {
            localError = "El nombre es obligatorio para el registro"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            localError = "Formato de email inválido"
            return false
        }
        if (password.length < 6) {
            localError = "La contraseña debe tener al menos 6 caracteres"
            return false
        }
        localError = null
        return true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fullscreen Background
        Image(
            painter = painterResource(id = R.drawable.fondo),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark Overlay (25% opacity)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        // Top UI Elements (Logo)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        }

        // Bottom Form
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isRegistering) "CREAR CUENTA" else "INICIAR SESIÓN",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            if (isRegistering) {
                CustomAuthTextField(
                    value = nombre,
                    onValueChange = { nombre = it; localError = null },
                    placeholder = "Nombre Completo",
                    icon = Icons.Default.Person
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            CustomAuthTextField(
                value = email,
                onValueChange = { email = it; localError = null },
                placeholder = "Email",
                icon = Icons.Default.Email
            )
            Spacer(modifier = Modifier.height(16.dp))
            CustomAuthTextField(
                value = password,
                onValueChange = { password = it; localError = null },
                placeholder = "Contraseña",
                icon = Icons.Default.VpnKey,
                isPassword = true
            )

            val displayError = localError ?: (if (state is AuthState.Error) state.message else null)
            if (displayError != null) {
                Text(
                    text = displayError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 12.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (state is AuthState.Loading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Button(
                    onClick = {
                        if (validateInputs()) {
                            if (isRegistering) onRegister(email, password, nombre)
                            else onLogin(email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = if (isRegistering) "REGISTRARSE" else "ENTRAR",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                
                TextButton(
                    onClick = {
                        isRegistering = !isRegistering
                        onReset()
                        localError = null
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = if (isRegistering) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun CustomAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.Gray) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        ),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
    )
}

@Composable
fun ProfileField(label: String, valStr: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall)
            Text(valStr, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

// 5. HELP / FAQ SCREEN (Ayuda / FAQ)
@Composable
fun AyudaFaqScreen() {
    val faqItems = listOf(
        FaqData(
            id = 1,
            pregunta = "¿Venenosa vs Peligrosa?",
            respuesta = "Casi TODAS las arañas chilenas son venenosas ya que usan el veneno para paralizar a sus presas. Sin embargo, muy pocas tienen un veneno PELIGROSO capaz de dañar gravemente la salud o tejidos de los humanos.\n\nEn Chile, solo la ARAÑA DE RINCÓN (Loxosceles laeta) y la ARAÑA DE TRIGO / Viuda Negra (Latrodectus mactans) se consideran de importancia médica extrema (altamente peligrosas). Las demás son aliadas pacíficas."
        ),
        FaqData(
            id = 2,
            pregunta = "¿Cuál es el rol de las arañas en el ecosistema?",
            respuesta = "Las arañas son controladores biológicos vitales. Se alimentan de moscas, mosquitos, polillas, zancudos y, crucialmente, la Araña Tigre (Scytodes globula) se alimenta de la temida Araña de Rincón.\n\nEliminar indiscriminadamente arañas silvestres provoca un aumento descontrolado de plagas indeseadas en nuestros hogares y campos."
        ),
        FaqData(
            id = 3,
            pregunta = "¿Qué hacer si me muerde una araña de rincón?",
            respuesta = "1. Mantén la calma y lava la zona afectada con agua y jabón.\n2. Aplica hielo local (el frío frena la acción necrótica del veneno).\n3. Captura la araña (viva o muerta) para facilitar su identificación médica.\n4. Dirígete INMEDIATAMENTE al servicio de urgencias más cercano (Hospital o Posta)."
        ),
        FaqData(
            id = 4,
            pregunta = "¿Cómo reconozco a la Araña Tigre chilena?",
            respuesta = "La Araña Tigre (Scytodes globula) tiene patas extremadamente largas, delgadas e hiladas con un diseño amarillo con negro muy característico. Se mueve lentamente y caza de forma nocturna escupiendo una red adhesiva sobre sus presas. Es inofensiva para humanos y una gran aliada."
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "¿Qué quieres aprender hoy?",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Herramientas de educación y prevención para la comunidad de Chile.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(faqItems) { faq ->
                FaqExpandableCard(faq = faq)
            }
        }
    }
}

data class FaqData(val id: Int, val pregunta: String, val respuesta: String)

@Composable
fun FaqExpandableCard(faq: FaqData) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("faq_card_${faq.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.pregunta,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = faq.respuesta,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// 6. FICHA TÉCNICA DIALOG (Visualizador de resultados de araña)
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FichaTecnicaDialog(
    especie: EspecieArana,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("Ficha Técnica Científica", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("ficha_dismiss_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with main tags
                item {
                    val colorAlert = when (especie.nivelPeligrosidad) {
                        "Extrema" -> MaterialTheme.colorScheme.error
                        "Alta" -> MaterialTheme.colorScheme.secondary
                        "Media" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = especie.nombreComun,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = especie.nombreCientifico,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Indicators Row
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Risk Indicator
                            Box(
                                modifier = Modifier
                                    .background(colorAlert.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .border(1.dp, colorAlert, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "RIESGO: ${especie.nivelPeligrosidad.uppercase()}",
                                    color = colorAlert,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            // Venomous Indicator
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (especie.venenosa) "SÍNTESIS DE VENENO" else "SIN GLÁNDULA",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Origin Indicator
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "ORIGEN: ${especie.origen.uppercase()}",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item { HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)) }

                // Family & Description card
                item {
                    FichaSection(
                        title = "Estructura Familiar",
                        content = "Familia: ${especie.familia}",
                        icon = Icons.Default.Info
                    )
                }

                item {
                    FichaSection(
                        title = "Descripción Física y Comportamiento",
                        content = especie.descripcion,
                        icon = Icons.Default.Description
                    )
                }

                item {
                    FichaSection(
                        title = "Hábitat Preferido",
                        content = especie.habitat,
                        icon = Icons.Default.Home
                    )
                }

                item {
                    FichaSection(
                        title = "Distribución Geográfica",
                        content = especie.distribucion,
                        icon = Icons.Default.PinDrop
                    )
                }

                // Extra bottom padding to ensure the last card is fully visible above navigation bars
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
fun FichaSection(
    title: String,
    content: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(content, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
        }
    }
}

// 7. ANALYSIS OVERLAY DIALOG (Analizador de carga con IA - HU04)
@Composable
fun AnalysisOverlayDialog(
    state: AnalysisState?,
    onDismiss: () -> Unit,
    onViewFicha: (EspecieArana) -> Unit
) {
    Dialog(
        onDismissRequest = { if (state !is AnalysisState.Loading) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (state) {
                    null, is AnalysisState.Idle -> {}
                    is AnalysisState.Loading -> {
                        // Loading Animation (HU04 - "Carga")
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(54.dp).testTag("analysis_loading")
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Analizando Araña...",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Arakno CL IA está consultando modelos de reconocimiento biológico...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                    is AnalysisState.Success -> {
                        val result = state.result

                        if (result.spiderFound && result.especie != null) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(54.dp).testTag("analysis_success_icon")
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "¡Identificación Exitosa!",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Identified Card
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        result.especie.nombreComun,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        result.especie.nombreCientifico,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Riesgo: ${result.especie.nivelPeligrosidad}",
                                        color = when (result.especie.nivelPeligrosidad) {
                                            "Extrema" -> MaterialTheme.colorScheme.error
                                            "Alta" -> MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.tertiary
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            if (result.mensajeOriginal != null) {
                                Text(
                                    text = result.mensajeOriginal,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.onSurface),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("dismiss_analysis")
                                ) {
                                    Text("Cerrar")
                                }
                                Button(
                                    onClick = { onViewFicha(result.especie) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.2f).testTag("view_ficha_analysis")
                                ) {
                                    Text("Ver Ficha", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // No spider found
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp).testTag("analysis_not_found_icon")
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No se detectó araña",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "El modelo no logró reconocer una araña con certeza suficiente en esta imagen. Intenta capturar una foto más cercana o con mejor luz.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("close_analysis_not_found")
                            ) {
                                Text("Volver a intentar")
                            }
                        }
                    }
                    is AnalysisState.Error -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(54.dp).testTag("analysis_error_icon")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ocurrió un error",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("close_analysis_error")
                        ) {
                            Text("Aceptar")
                        }
                    }
                }
            }
        }
    }
}
