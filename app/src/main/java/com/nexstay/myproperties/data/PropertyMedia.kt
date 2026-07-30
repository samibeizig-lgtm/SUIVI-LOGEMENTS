package com.nexstay.myproperties.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Photo ou vidéo attachée à un logement. Le fichier est copié dans filesDir/media/<propertyId>/. */
@Entity(tableName = "property_media")
data class PropertyMedia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val fileName: String,
    val isVideo: Boolean,
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface MediaDao {

    @Query("SELECT * FROM property_media WHERE propertyId = :propertyId ORDER BY addedAt ASC")
    fun observeForProperty(propertyId: Long): Flow<List<PropertyMedia>>

    @Query("SELECT * FROM property_media")
    suspend fun getAll(): List<PropertyMedia>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: PropertyMedia): Long

    @Delete
    suspend fun delete(media: PropertyMedia)

    @Query("DELETE FROM property_media WHERE propertyId = :propertyId")
    suspend fun deleteForProperty(propertyId: Long)

    @Query("DELETE FROM property_media")
    suspend fun deleteAll()
}
