package com.nexstay.myproperties.ui

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexstay.myproperties.MyPropertiesApp
import com.nexstay.myproperties.data.Property
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class PropertyViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as MyPropertiesApp).database.propertyDao()

    val properties: StateFlow<List<Property>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun property(id: Long): Flow<Property?> = dao.observeById(id)

    suspend fun getProperty(id: Long): Property? = dao.getById(id)

    /**
     * Enregistre le logement (création ou mise à jour) puis tente de géocoder
     * l'adresse en arrière-plan pour l'afficher sur la carte.
     */
    fun save(property: Property, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = if (property.id != 0L) dao.getById(property.id) else null
            val addressChanged = existing == null || existing.address != property.address
            val toSave = property.copy(
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                latitude = if (addressChanged) null else property.latitude,
                longitude = if (addressChanged) null else property.longitude
            )
            val id = if (toSave.id == 0L) dao.insert(toSave) else {
                dao.update(toSave)
                toSave.id
            }
            onSaved(id)
            if (toSave.latitude == null && toSave.address.isNotBlank()) {
                geocode(id, toSave.address)
            }
        }
    }

    private suspend fun geocode(id: Long, address: String) {
        val location = withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                Geocoder(getApplication(), Locale.getDefault())
                    .getFromLocationName(address, 1)
                    ?.firstOrNull()
            }.getOrNull()
        } ?: return
        dao.getById(id)?.let {
            dao.update(it.copy(latitude = location.latitude, longitude = location.longitude))
        }
    }

    fun delete(property: Property) {
        viewModelScope.launch { dao.delete(property) }
    }
}
