package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import java.text.SimpleDateFormat
import java.util.*

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

    // Scaffold with clean cohesive dark theme
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = com.example.ui.theme.JungleDarkBackground,
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
                            if (match != null) viewModel.selectEspecie(match)
                        }
                    )
                    "profile" -> {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        PerfilScreen(
                            usuario = usuario,
                            sightingCount = avistamientos.size,
                            onSaveProfile = { nom, cor, cel -> viewModel.updateProfile(nom, cor, cel) },
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
        containerColor = com.example.ui.theme.FoliageDarkSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.05f))
    ) {
        NavigationBarItem(
            selected = activeTab == "home",
            onClick = { onTabSelected("home") },
            label = { Text("Inicio", fontSize = 11.sp) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = com.example.ui.theme.MintHighlight,
                selectedTextColor = com.example.ui.theme.MintHighlight,
                unselectedIconColor = Color(0x8AFFFFFF),
                unselectedTextColor = Color(0x8AFFFFFF),
                indicatorColor = com.example.ui.theme.CanopyOverlay
            ),
            modifier = Modifier.testTag("nav_home")
        )
        NavigationBarItem(
            selected = activeTab == "guide",
            onClick = { onTabSelected("guide") },
            label = { Text("Guía", fontSize = 11.sp) },
            icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = com.example.ui.theme.MintHighlight,
                selectedTextColor = com.example.ui.theme.MintHighlight,
                unselectedIconColor = Color(0x8AFFFFFF),
                unselectedTextColor = Color(0x8AFFFFFF),
                indicatorColor = com.example.ui.theme.CanopyOverlay
            ),
            modifier = Modifier.testTag("nav_guide")
        )
        NavigationBarItem(
            selected = activeTab == "journal",
            onClick = { onTabSelected("journal") },
            label = { Text("Diario", fontSize = 11.sp) },
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = com.example.ui.theme.MintHighlight,
                selectedTextColor = com.example.ui.theme.MintHighlight,
                unselectedIconColor = Color(0x8AFFFFFF),
                unselectedTextColor = Color(0x8AFFFFFF),
                indicatorColor = com.example.ui.theme.CanopyOverlay
            ),
            modifier = Modifier.testTag("nav_journal")
        )
        NavigationBarItem(
            selected = activeTab == "profile",
            onClick = { onTabSelected("profile") },
            label = { Text("Perfil", fontSize = 11.sp) },
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = com.example.ui.theme.MintHighlight,
                selectedTextColor = com.example.ui.theme.MintHighlight,
                unselectedIconColor = Color(0x8AFFFFFF),
                unselectedTextColor = Color(0x8AFFFFFF),
                indicatorColor = com.example.ui.theme.CanopyOverlay
            ),
            modifier = Modifier.testTag("nav_profile")
        )
        NavigationBarItem(
            selected = activeTab == "help",
            onClick = { onTabSelected("help") },
            label = { Text("Ayuda", fontSize = 11.sp) },
            icon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = com.example.ui.theme.MintHighlight,
                selectedTextColor = com.example.ui.theme.MintHighlight,
                unselectedIconColor = Color(0x8AFFFFFF),
                unselectedTextColor = Color(0x8AFFFFFF),
                indicatorColor = com.example.ui.theme.CanopyOverlay
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
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.BarkDarkCard),
        border = BorderStroke(1.dp, if (isConfigured) com.example.ui.theme.SafetyLeafGreen else com.example.ui.theme.WarningAmber.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (isConfigured) com.example.ui.theme.SafetyLeafGreen else com.example.ui.theme.WarningAmber, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (isConfigured) "Soporte Supabase: Activo" else "Soporte Supabase: Integrado (Modo Local)",
                    color = com.example.ui.theme.CrispDewDropWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isConfigured) supabaseUrl else "Detección guardará datos localmente y replicará a Supabase al configurar claves.",
                    color = com.example.ui.theme.FernMutedGray,
                    fontSize = 11.sp,
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.BarkDarkCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, com.example.ui.theme.MossGreenAccent.copy(alpha = 0.2f))
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
                            color = com.example.ui.theme.MintHighlight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Has registrado $sightingCount encuentro(s) de arañas.",
                            color = com.example.ui.theme.CrispDewDropWhite.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(com.example.ui.theme.CanopyOverlay, CircleShape)
                            .border(1.dp, com.example.ui.theme.MossGreenAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NaturePeople,
                            contentDescription = null,
                            tint = com.example.ui.theme.MintHighlight
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
                        .size(171.dp)
                        .border(3.dp, com.example.ui.theme.MintHighlight.copy(alpha = 0.4f), CircleShape)
                        .padding(8.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(com.example.ui.theme.MintHighlight, com.example.ui.theme.ForestGreenPrimary)
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
                            tint = com.example.ui.theme.JungleDarkBackground,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ESCANEAR ARAÑA",
                            color = com.example.ui.theme.JungleDarkBackground,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Apunta a la araña para identificarla",
                    color = com.example.ui.theme.FernMutedGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        // Fast Access Navigation Cards Grid
        item {
            Text(
                text = "Accesos Rápidos",
                color = com.example.ui.theme.CrispDewDropWhite,
                fontSize = 16.sp,
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
                        .height(115.dp)
                        .clickable { onNavigateToGuide() },
                    colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.BarkDarkCard)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = com.example.ui.theme.MintHighlight
                        )
                        Column {
                            Text(
                                "Guía de Especies",
                                color = com.example.ui.theme.CrispDewDropWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                "Fichas técnicas y hábitats",
                                color = com.example.ui.theme.FernMutedGray,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Journal access
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(115.dp)
                        .clickable { onNavigateToJournal() },
                    colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.BarkDarkCard)
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
                            tint = com.example.ui.theme.MintHighlight
                        )
                        Column {
                            Text(
                                "Mi Diario",
                                color = com.example.ui.theme.CrispDewDropWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                "Tus capturas guardadas",
                                color = com.example.ui.theme.FernMutedGray,
                                fontSize = 10.sp,
                                maxLines = 1,
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
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.BarkDarkCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(com.example.ui.theme.MintHighlight.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = com.example.ui.theme.MintHighlight
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¿Venenoso vs Peligroso?",
                            color = com.example.ui.theme.CrispDewDropWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Aprende la diferencia en la guía de ayuda",
                            color = com.example.ui.theme.FernMutedGray,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowForward,
                        contentDescription = null,
                        tint = com.example.ui.theme.CrispDewDropWhite.copy(alpha = 0.4f),
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
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Arañas más comunes registradas en Chile y su nivel de riesgo.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Buscar por nombre, familia...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.LightGray)
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("search_bar"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = com.example.ui.theme.BarkDarkCard,
                unfocusedContainerColor = com.example.ui.theme.BarkDarkCard,
                focusedBorderColor = com.example.ui.theme.MintHighlight,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Filter chips (Endémico, Nativo, Exótico)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("Todos", "Endémico", "Nativo", "Exótico")
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected) com.example.ui.theme.MintHighlight else com.example.ui.theme.FoliageDarkSurface,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("filter_chip_$filter"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) com.example.ui.theme.JungleDarkBackground else com.example.ui.theme.CrispDewDropWhite.copy(alpha = 0.8f),
                        fontSize = 12.sp,
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
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No se encontraron especies", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
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
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.BarkDarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stylized safety warning indicator icon
            val colorAlert = when (especie.nivelPeligrosidad) {
                "Extrema" -> com.example.ui.theme.HazardToxicityRed
                "Alta" -> com.example.ui.theme.WarningAmber
                "Media" -> Color(0xFFF39C12) // Scientific warning gold
                else -> com.example.ui.theme.SafetyLeafGreen
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(com.example.ui.theme.CanopyOverlay, CircleShape)
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
                    color = com.example.ui.theme.CrispDewDropWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = especie.nombreCientifico,
                    color = com.example.ui.theme.FernMutedGray,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Origin Badge
                    Box(
                        modifier = Modifier
                            .background(com.example.ui.theme.JungleDarkBackground, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(especie.origen, color = com.example.ui.theme.MintHighlight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
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
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Historial de encuentros identificados en terreno.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
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
                    Icon(Icons.Default.Camera, contentDescription = null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aún no tienes encuentros registrados",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Usa el scanner de cámara para registrar tu primera araña",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp,
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
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.BarkDarkCard)
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
                        tint = com.example.ui.theme.MintHighlight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = avistamiento.ubicacionNombre,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp).testTag("delete_sighting_${avistamiento.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sighting header
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(com.example.ui.theme.CanopyOverlay, RoundedCornerShape(8.dp))
                        .border(1.dp, com.example.ui.theme.MintHighlight.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.NaturePeople, contentDescription = null, tint = com.example.ui.theme.MintHighlight, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = avistamiento.resultadoNombreComun,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = avistamiento.resultadoEspecie,
                        color = com.example.ui.theme.FernMutedGray,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .background(com.example.ui.theme.SafetyLeafGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${(avistamiento.confianza * 100).toInt()}% Confianza",
                            color = com.example.ui.theme.SafetyLeafGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateString,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp
                    )
                }
            }

            if (avistamiento.comentarios.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = avistamiento.comentarios,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(com.example.ui.theme.JungleDarkBackground, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Ver ficha científica completa →",
                color = com.example.ui.theme.MintHighlight,
                fontSize = 12.sp,
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
    onSaveProfile: (String, String, String) -> Unit,
    onLogout: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    var username by remember(usuario?.nombre) { mutableStateOf(usuario?.nombre ?: "") }
    var email by remember(usuario?.correo) { mutableStateOf(usuario?.correo ?: "") }
    var phone by remember(usuario?.celular) { mutableStateOf(usuario?.celular ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Perfil del Explorador",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Count box with elegant visual design
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.BarkDarkCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(com.example.ui.theme.MintHighlight.copy(alpha = 0.15f), CircleShape)
                        .border(1.5.dp, com.example.ui.theme.MintHighlight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = com.example.ui.theme.MintHighlight, modifier = Modifier.size(44.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = username,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Miembro activo de Arakno CL",
                    color = com.example.ui.theme.FernMutedGray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = sightingCount.toString(),
                            color = com.example.ui.theme.MintHighlight,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Encuentros",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Chile",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "Territorio",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Editable details form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.BarkDarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_edit"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = com.example.ui.theme.MintHighlight
                        )
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo electrónico") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_email_edit"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = com.example.ui.theme.MintHighlight
                        )
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Celular de contacto") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_phone_edit"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = com.example.ui.theme.MintHighlight
                        )
                    )

                    Button(
                        onClick = {
                            onSaveProfile(username, email, phone)
                            isEditing = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.MintHighlight, contentColor = com.example.ui.theme.JungleDarkBackground),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("profile_save_button")
                    ) {
                        Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                    }
                } else {
                    ProfileField(label = "Correo electrónico", valStr = email, icon = Icons.Default.Email)
                    ProfileField(label = "Celular de contacto", valStr = phone, icon = Icons.Default.Phone)

                    Button(
                        onClick = { isEditing = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("profile_edit_toggle")
                    ) {
                        Text("Editar información de contacto", fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.ui.theme.WarningAmber),
                        border = BorderStroke(1.dp, com.example.ui.theme.WarningAmber.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar Sesión")
                    }
                }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.VpnKey,
            contentDescription = null,
            tint = com.example.ui.theme.MintHighlight,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isRegistering) "Crear Cuenta Supabase" else "Iniciar Sesión Supabase",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isRegistering) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; localError = null },
                label = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; localError = null },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; localError = null },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        val displayError = localError ?: (if (state is AuthState.Error) state.message else null)
        if (displayError != null) {
            Text(displayError, color = com.example.ui.theme.WarningAmber, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state is AuthState.Loading) {
            CircularProgressIndicator(color = com.example.ui.theme.MintHighlight)
        } else {
            Button(
                onClick = {
                    if (validateInputs()) {
                        if (isRegistering) onRegister(email, password, nombre)
                        else onLogin(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.MintHighlight, contentColor = com.example.ui.theme.JungleDarkBackground)
            ) {
                Text(if (isRegistering) "Registrarse" else "Entrar", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { 
                isRegistering = !isRegistering 
                onReset()
                localError = null
            }) {
                Text(
                    if (isRegistering) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate",
                    color = com.example.ui.theme.MintHighlight.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ProfileField(label: String, valStr: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            Text(valStr, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Herramientas de educación y prevención para la comunidad de Chile.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
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
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.BarkDarkCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.pregunta,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = com.example.ui.theme.MintHighlight
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = faq.respuesta,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
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
            containerColor = com.example.ui.theme.JungleDarkBackground,
            topBar = {
                TopAppBar(
                    title = { Text("Ficha Técnica Científica", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("ficha_dismiss_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = com.example.ui.theme.FoliageDarkSurface)
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
                        "Extrema" -> com.example.ui.theme.HazardToxicityRed
                        "Alta" -> com.example.ui.theme.WarningAmber
                        "Media" -> Color(0xFFF39C12)
                        else -> com.example.ui.theme.SafetyLeafGreen
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = especie.nombreComun,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = especie.nombreCientifico,
                            color = com.example.ui.theme.MintHighlight,
                            fontSize = 16.sp,
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
                                    text = "Riesgo: ${especie.nivelPeligrosidad.uppercase()}",
                                    color = colorAlert,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            // Venomous Indicator
                            Box(
                                modifier = Modifier
                                    .background(com.example.ui.theme.CanopyOverlay, RoundedCornerShape(6.dp))
                                    .border(1.dp, com.example.ui.theme.MintHighlight, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (especie.venenosa) "SÍNTESIS DE VENENO" else "SIN GLÁNDULA",
                                    color = com.example.ui.theme.MintHighlight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Origin Indicator
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "ORIGEN: ${especie.origen.uppercase()}",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item { HorizontalDivider(color = Color.White.copy(alpha = 0.08f)) }

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
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.BarkDarkCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = com.example.ui.theme.MintHighlight, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(content, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 18.sp)
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
            colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.BarkDarkCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, com.example.ui.theme.MintHighlight.copy(alpha = 0.2f))
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
                            color = com.example.ui.theme.MintHighlight,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(54.dp).testTag("analysis_loading")
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Analizando Araña...",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Arakno CL IA está consultando modelos de reconocimiento biológico...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
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
                                tint = com.example.ui.theme.SafetyLeafGreen,
                                modifier = Modifier.size(54.dp).testTag("analysis_success_icon")
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "¡Identificación Exitosa!",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Identified Card
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.JungleDarkBackground)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        result.especie.nombreComun,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        result.especie.nombreCientifico,
                                        color = com.example.ui.theme.MintHighlight,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Riesgo: ${result.especie.nivelPeligrosidad}",
                                        color = when (result.especie.nivelPeligrosidad) {
                                            "Extrema" -> com.example.ui.theme.HazardToxicityRed
                                            "Alta" -> com.example.ui.theme.WarningAmber
                                            else -> com.example.ui.theme.SafetyLeafGreen
                                        },
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            if (result.mensajeOriginal != null) {
                                Text(
                                    text = result.mensajeOriginal,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("dismiss_analysis")
                                ) {
                                    Text("Cerrar")
                                }
                                Button(
                                    onClick = { onViewFicha(result.especie) },
                                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.MintHighlight, contentColor = com.example.ui.theme.JungleDarkBackground),
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
                                tint = com.example.ui.theme.MintHighlight,
                                modifier = Modifier.size(54.dp).testTag("analysis_not_found_icon")
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No se detectó araña",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "El modelo no logró reconocer una araña con certeza suficiente en esta imagen. Intenta capturar una foto más cercana o con mejor luz.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.MintHighlight, contentColor = com.example.ui.theme.JungleDarkBackground),
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
                            tint = Color.Red,
                            modifier = Modifier.size(54.dp).testTag("analysis_error_icon")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ocurrió un error",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (state as AnalysisState.Error).message,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.MintHighlight, contentColor = com.example.ui.theme.JungleDarkBackground),
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
