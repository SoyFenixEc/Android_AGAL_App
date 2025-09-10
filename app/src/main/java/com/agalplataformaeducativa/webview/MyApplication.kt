package com.agalplataformaeducativa.webview // ✅ ¡PAQUETE CORRECTO!

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp // ✅ ¡IMPORTA ESTO!

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this) // ✅ ¡Debe estar PRIMERO!
        MobileAds.initialize(this)
    }
}

