package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val correo: String,
    val fechaRegistro: Long = System.currentTimeMillis(),
    val fotoPerfil: String = "" // Base64 or URI
) : Serializable

@Entity(tableName = "especies_arana")
data class EspecieArana(
    @PrimaryKey val nombreCientifico: String,
    val nombreComun: String,
    val familia: String,
    val descripcion: String,
    val nivelPeligrosidad: String, // "Alta", "Media", "Ninguna/Inofensiva"
    val venenosa: Boolean,
    val habitat: String,
    val distribucion: String, // e.g. "Todo Chile"
    val origen: String // "Endémico", "Nativo", "Exótico"
) : Serializable

@Entity(tableName = "avistamientos")
data class Avistamiento(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idUsuario: Int = 1,
    val urlImagen: String, // local file path, URI, or base64 placeholder
    val fechaCaptura: Long = System.currentTimeMillis(),
    val latitud: Double = -33.4372, // Santiago default coordinates
    val longitud: Double = -70.6506,
    val ubicacionNombre: String = "Chile", // e.g., "Lampa, RM" or "Santiago, RM"
    val confianza: Double = 0.92,
    val resultadoEspecie: String, // Match against nombreCientifico
    val resultadoNombreComun: String,
    val comentarios: String = ""
) : Serializable
