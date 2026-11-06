package com.bishal.adaptivecamera.lut.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LutDao {

    @Query("SELECT * FROM luts ORDER BY isFavorite DESC, name ASC")
    fun getAllLutsFlow(): Flow<List<LutEntity>>

    @Query("SELECT * FROM luts WHERE id = :id LIMIT 1")
    suspend fun getLutById(id: String): LutEntity?

    @Query("SELECT * FROM luts WHERE contentHash = :hash LIMIT 1")
    suspend fun getLutByContentHash(hash: String): LutEntity?

    @Query("SELECT * FROM luts WHERE sourceType = 'BUILT_IN'")
    suspend fun getBuiltInLuts(): List<LutEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLut(lut: LutEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(luts: List<LutEntity>)

    @Update
    suspend fun updateLut(lut: LutEntity)

    @Query("UPDATE luts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM luts WHERE id = :id AND sourceType = 'IMPORTED'")
    suspend fun deleteImportedLut(id: String): Int

    @Query("SELECT COUNT(*) FROM luts")
    suspend fun getLutCount(): Int
}
