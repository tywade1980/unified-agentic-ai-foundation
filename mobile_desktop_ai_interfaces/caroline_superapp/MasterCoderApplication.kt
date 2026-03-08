package com.enhanced.codeassist

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MasterCoderApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialize Master Coder Engine
        initializeMasterCoder()
        
        Timber.d("🚀 MasterCoder Application initialized successfully")
    }
    
    private fun initializeMasterCoder() {
        try {
            // The engine will be initialized when first accessed through Hilt
            MasterCoderEngine.initialize(this)
            Timber.d("MasterCoder engine initialization scheduled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize MasterCoder engine")
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // Cleanup resources
        MasterCoderEngine.getInstance()?.cleanup()
    }
}

