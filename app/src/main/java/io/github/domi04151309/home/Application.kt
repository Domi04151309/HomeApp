package io.github.domi04151309.home

import android.content.Context
import com.google.android.material.color.DynamicColors
import io.github.domi04151309.home.helpers.LocaleHelper

class Application : android.app.Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(base))
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
