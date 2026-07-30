package com.nexstay.myproperties

import android.app.Application
import com.nexstay.myproperties.data.PropertyDatabase
import org.osmdroid.config.Configuration

class MyPropertiesApp : Application() {

    val database: PropertyDatabase by lazy { PropertyDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        // osmdroid exige un user agent identifiant l'application pour les tuiles OSM.
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidBasePath = getExternalFilesDir("osmdroid") ?: filesDir
    }
}
