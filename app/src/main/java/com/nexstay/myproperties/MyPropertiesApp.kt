package com.nexstay.myproperties

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.nexstay.myproperties.data.PropertyDatabase

class MyPropertiesApp : Application(), ImageLoaderFactory {

    val database: PropertyDatabase by lazy { PropertyDatabase.getInstance(this) }

    // Chargeur d'images capable d'extraire une miniature des vidéos.
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
