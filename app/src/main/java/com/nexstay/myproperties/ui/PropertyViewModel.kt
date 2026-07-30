package com.nexstay.myproperties.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexstay.myproperties.MyPropertiesApp
import com.nexstay.myproperties.data.Property
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PropertyViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as MyPropertiesApp).database.propertyDao()

    val properties: StateFlow<List<Property>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun property(id: Long): Flow<Property?> = dao.observeById(id)

    /** Enregistre le logement (création ou mise à jour). La position vient du placement manuel sur la carte. */
    fun save(property: Property, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = if (property.id != 0L) dao.getById(property.id) else null
            val toSave = property.copy(
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            val id = if (toSave.id == 0L) dao.insert(toSave) else {
                dao.update(toSave)
                toSave.id
            }
            onSaved(id)
        }
    }

    fun delete(property: Property) {
        viewModelScope.launch { dao.delete(property) }
    }
}
