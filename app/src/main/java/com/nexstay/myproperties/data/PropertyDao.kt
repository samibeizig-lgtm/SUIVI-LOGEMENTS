package com.nexstay.myproperties.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {

    @Query("SELECT * FROM properties ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Property>>

    @Query("SELECT * FROM properties WHERE id = :id")
    fun observeById(id: Long): Flow<Property?>

    @Query("SELECT * FROM properties WHERE id = :id")
    suspend fun getById(id: Long): Property?

    @Query("SELECT * FROM properties")
    suspend fun getAll(): List<Property>

    @Query("DELETE FROM properties")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(property: Property): Long

    @Update
    suspend fun update(property: Property)

    @Delete
    suspend fun delete(property: Property)
}
