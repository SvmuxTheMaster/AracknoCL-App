package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AraknoDao {
    // --- Usuario Queries ---
    @Query("SELECT * FROM usuarios ORDER BY id LIMIT 1")
    fun getUsuario(): Flow<Usuario?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuario(usuario: Usuario)

    @Update
    suspend fun updateUsuario(usuario: Usuario)

    @Query("DELETE FROM usuarios")
    suspend fun clearUsuario()

    // --- Especie de Araña Queries ---
    @Query("SELECT * FROM especies_arana ORDER BY nombreComun ASC")
    fun getAllEspecies(): Flow<List<EspecieArana>>

    @Query("SELECT * FROM especies_arana WHERE nombreCientifico = :nombreCientifico LIMIT 1")
    suspend fun getEspecieByCientifico(nombreCientifico: String): EspecieArana?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEspecie(especie: EspecieArana)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEspecies(especies: List<EspecieArana>)

    @Query("DELETE FROM especies_arana")
    suspend fun deleteAllEspecies()

    // --- Avistamiento Queries ---
    @Query("SELECT * FROM avistamientos ORDER BY fechaCaptura DESC")
    fun getAllAvistamientos(): Flow<List<Avistamiento>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvistamiento(avistamiento: Avistamiento)

    @Query("DELETE FROM avistamientos WHERE id = :id")
    suspend fun deleteAvistamientoById(id: Int)
}
