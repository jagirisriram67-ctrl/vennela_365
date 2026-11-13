package com.vennela365.viewer

import android.app.Application
import com.vennela365.viewer.storage.VennelaCacheManager

class VennelaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        VennelaCacheManager.initialize(this)
    }
}
