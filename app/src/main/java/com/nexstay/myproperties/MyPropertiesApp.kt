package com.nexstay.myproperties

import android.app.Application
import com.nexstay.myproperties.data.PropertyDatabase
import org.osmdroid.config.Configuration

class MyPropertiesApp : Application() {

    val database: PropertyDatabase by lazy { PropertyDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        // osmdroid exige un user agent identifiant l'application pour les tuiles OSM.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = getExternalFilesDir("osmdroid") ?: filesDir
            // Cache longue durée pour que la carte reste consultable hors-ligne.
            expirationOverrideDuration = 365L * 24 * 60 * 60 * 1000
            tileFileSystemCacheMaxBytes = 600L * 1024 * 1024
            tileFileSystemCacheTrimBytes = 500L * 1024 * 1024
        }
    }
}
