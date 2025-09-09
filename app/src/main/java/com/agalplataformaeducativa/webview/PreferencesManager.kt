package com.agalplataformaeducativa.webview

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    var savedSubdomain: String?
        get() = prefs.getString("SUBDOMAIN", null)
        set(value) = prefs.edit().putString("SUBDOMAIN", value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("IS_FIRST_LAUNCH", true)
        set(value) = prefs.edit().putBoolean("IS_FIRST_LAUNCH", value).apply()
}