package com.nexstay.myproperties

import android.app.Application
import com.nexstay.myproperties.data.PropertyDatabase

class MyPropertiesApp : Application() {

    val database: PropertyDatabase by lazy { PropertyDatabase.getInstance(this) }
}
