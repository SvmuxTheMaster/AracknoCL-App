package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take

class AraknoRepository(private val dao: AraknoDao) {

    val usuario: Flow<Usuario?> = dao.getUsuario()
    val allEspecies: Flow<List<EspecieArana>> = dao.getAllEspecies()
    val allAvistamientos: Flow<List<Avistamiento>> = dao.getAllAvistamientos()

    suspend fun verifyAndSeedDatabase() {
        // We'll check if the species table is empty, and seed it if so
        val currentEspecies = dao.getAllEspecies().take(1).firstOrNull()
        if (currentEspecies.isNullOrEmpty()) {
            val initialEspecies = listOf(
                EspecieArana(
                    nombreCientifico = "Loxosceles laeta",
                    nombreComun = "Araña de rincón",
                    familia = "Sicariidae",
                    descripcion = "Es de color marrón brillante con una marca característica en forma de violín invertido en el cefalotórax. Extremadamente rápida y de hábitos nocturnos. Su mordedura provoca loxoscelismo cutáneo o visceral, con potencial dermonecrótico y hemolítico grave.",
                    nivelPeligrosidad = "Extrema",
                    venenosa = true,
                    habitat = "Detrás de cuadros, rincones oscuros, closets, zócalos y en general zonas interiores del hogar de baja ventilación.",
                    distribucion = "En todo el territorio nacional urbano y semiurbano chileno.",
                    origen = "Exótico"
                ),
                EspecieArana(
                    nombreCientifico = "Latrodectus mactans",
                    nombreComun = "Araña de trigo / Viuda negra del sur",
                    familia = "Theridiidae",
                    descripcion = "Cuerpo negro brillante, de abdomen globular con notorias manchas rojas o anaranjadas (a menudo en forma de reloj de arena). Su veneno posee alfa-latrotoxina neurotóxica, causando contracciones musculares severas, sudoración y dolor agudo en humanos.",
                    nivelPeligrosidad = "Extrema",
                    venenosa = true,
                    habitat = "Zonas exteriores rurales, debajo de rocas, rastrojos de trigo, pilas de leña y madrigueras a nivel del suelo.",
                    distribucion = "Gran extensión silvestre de la zona centro y sur de Chile (Coquimbo a la Región de Los Lagos).",
                    origen = "Nativo"
                ),
                EspecieArana(
                    nombreCientifico = "Scytodes globula",
                    nombreComun = "Araña tigre / Araña escupidora",
                    familia = "Scytodidae",
                    descripcion = "Fácilmente reconocible por su patrón de franjas amarillas y negras en sus patas largas y delgadas. Captura a sus presas proyectando una goma viscosa mezclada con veneno que inmoviliza a la víctima a distancia. Es el principal depredador y aliado biológico contra la araña de rincón doméstica.",
                    nivelPeligrosidad = "Inofensiva",
                    venenosa = true, // technically venomous but absolutely harmless/helpful to humans
                    habitat = "Interior de viviendas en muros, detrás de muebles altos, bodegas domésticas.",
                    distribucion = "Todo el territorio de Chile continental urbano.",
                    origen = "Nativo"
                ),
                EspecieArana(
                    nombreCientifico = "Sicarius terrosus",
                    nombreComun = "Araña sicario / Araña de arena chilena",
                    familia = "Sicariidae",
                    descripcion = "Cuerpo aplanado de color marrón tierra. Tiene el comportamiento asombroso de autorretenido u ocultación bajo la arena o polvo para mimetizarse por completo. Su veneno es necrotizante muy potente, pero al habitar desiertos sus encuentros son extremadamente raros.",
                    nivelPeligrosidad = "Alta",
                    venenosa = true,
                    habitat = "Entornos áridos desérticos, debajo de piedras planas, madrigueras secas de tierra arenosa.",
                    distribucion = "Zona norte y centro semiárido (Región de Antofagasta hasta Valparaíso).",
                    origen = "Endémico"
                ),
                EspecieArana(
                    nombreCientifico = "Grammostola rosea",
                    nombreComun = "Tarántula chilena rosada / Araña pollito",
                    familia = "Theraphosidae",
                    descripcion = "Araña de gran tamaño con abundantes vellosidades de tonalidades cafés y reflejos cobrizos o rosados. Su mordedura es de baja toxicidad (similar a una picadura de abeja), y prefiere defenderse soltando pelos urticantes molestos que causan alergias en ojos o piel.",
                    nivelPeligrosidad = "Inofensiva",
                    venenosa = true,
                    habitat = "Madrigueras profundas bajo rocas o troncos secos en cerros y matorral xerofítico.",
                    distribucion = "Norte y zona central de Chile (Atacama a Biobío).",
                    origen = "Endémico"
                ),
                EspecieArana(
                    nombreCientifico = "Steatoda nobilis",
                    nombreComun = "Falsa viuda negra",
                    familia = "Theridiidae",
                    descripcion = "Similar en morfología a una viuda negra pero de un color castaño rojizo con patrones dorsales blanquecinos simulando un escudo o calavera. Su mordedura causa dolor localizado y náuseas leves (estearodismo). Es una especie introducida de rápida expansión.",
                    nivelPeligrosidad = "Media",
                    venenosa = true,
                    habitat = "Jardines residenciales urbanos, bodegas, marcos de ventanas exteriores.",
                    distribucion = "Chile central e insular (muy común en Valparaíso y Santiago).",
                    origen = "Exótico"
                )
            )
            dao.insertEspecies(initialEspecies)
        }

        // Also check if initial user exists
        val currentUsuario = dao.getUsuario().take(1).firstOrNull()
        if (currentUsuario == null) {
            dao.insertUsuario(
                Usuario(
                    id = 1,
                    nombre = "Explorador de Chile",
                    correo = "explorador@arakno.cl",
                    celular = "+56 9 8765 4321",
                    fechaRegistro = System.currentTimeMillis() - 86400000 * 30, // 30 days ago
                    fotoPerfil = ""
                )
            )
        }
    }

    suspend fun getEspecieByCientifico(nombreCientifico: String): EspecieArana? {
        return dao.getEspecieByCientifico(nombreCientifico)
    }

    suspend fun insertUsuario(usuario: Usuario) {
        dao.insertUsuario(usuario)
        // Sync with Supabase backend
        try {
            com.example.network.SupabaseManager.upsertUsuario(usuario)
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Failed to sync user to Supabase", e)
        }
    }

    suspend fun insertAvistamiento(avistamiento: Avistamiento) {
        dao.insertAvistamiento(avistamiento)
        // Sync with Supabase backend
        try {
            com.example.network.SupabaseManager.insertAvistamiento(avistamiento)
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Failed to sync avistamiento to Supabase", e)
        }
    }

    suspend fun deleteAvistamientoById(id: Int) {
        dao.deleteAvistamientoById(id)
        // Sync deletion with Supabase backend
        try {
            com.example.network.SupabaseManager.deleteAvistamiento(id)
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Failed to delete avistamiento from Supabase", e)
        }
    }

    // --- Authentication ---

    suspend fun signIn(email: String, pass: String): String? {
        return com.example.network.SupabaseManager.signIn(email, pass)
    }

    suspend fun signUp(email: String, pass: String): org.json.JSONObject? {
        return com.example.network.SupabaseManager.signUp(email, pass)
    }

    suspend fun getUserProfile(email: String): org.json.JSONObject? {
        return com.example.network.SupabaseManager.fetchUserProfile(email)
    }

    fun signOut() {
        com.example.network.SupabaseManager.signOut()
    }
}
