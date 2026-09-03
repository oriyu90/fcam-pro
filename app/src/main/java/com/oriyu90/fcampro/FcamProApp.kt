package com.oriyu90.fcampro

import android.app.Application
import com.oriyu90.fcampro.core.AppSettings
import com.oriyu90.fcampro.core.LocaleController

class FcamProApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Re-assert the stored language on cold start. AppCompat also persists its own
        // copy (autoStoreLocales); applying our value keeps the two in sync after an
        // app-data backup restore.
        val stored = AppSettings.get(this).languageTag
        if (stored != LocaleController.currentTag()) {
            LocaleController.apply(stored)
        }
    }
}
