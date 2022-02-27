package io.github.domi04151309.home.helpers

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.preference.PreferenceManager
import androidx.core.content.ContextCompat
import io.github.domi04151309.home.R

internal object Theme {

    fun set(context: Context) {
        when (PreferenceManager.getDefaultSharedPreferences(context).getString("theme", "light")) {
            "light" -> {
                context.setTheme(R.style.AppTheme27)
                recent(context, R.color.colorPrimary)
                context.setTheme(R.style.AppThemePatch)
            }
            "dark" -> {
                context.setTheme(R.style.AppThemeDark)
                recent(context, R.color.colorPrimaryDark)
                context.setTheme(R.style.AppThemePatchDark)
            }
            "black" -> {
                context.setTheme(R.style.AppThemeBlack)
                recent(context, R.color.colorPrimaryBlack)
                context.setTheme(R.style.AppThemePatchDark)
            }
            "auto" -> {
                when (context.resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)) {
                    Configuration.UI_MODE_NIGHT_YES -> {
                        context.setTheme(R.style.AppThemeDark)
                        recent(context, R.color.colorPrimaryDark)
                        context.setTheme(R.style.AppThemePatchDark)
                    }
                    else -> {
                        context.setTheme(R.style.AppTheme27)
                        recent(context, R.color.colorPrimary)
                        context.setTheme(R.style.AppThemePatch)
                    }
                }
            }
            else -> {
                context.setTheme(R.style.AppTheme27)
                recent(context, R.color.colorPrimary)
                context.setTheme(R.style.AppThemePatch)
            }
        }
    }

    fun setNoActionBar(context: Context) {
        when (PreferenceManager.getDefaultSharedPreferences(context).getString("theme", "light")) {
            "light" -> {
                context.setTheme(R.style.AppTheme_NoActionBar)
                recent(context, R.color.colorPrimary)
                context.setTheme(R.style.AppThemePatch)
            }
            "dark" -> {
                context.setTheme(R.style.AppThemeDark_NoActionBar)
                recent(context, R.color.colorPrimaryDark)
                context.setTheme(R.style.AppThemePatchDark)
            }
            "black" -> {
                context.setTheme(R.style.AppThemeBlack_NoActionBar)
                recent(context, R.color.colorPrimaryBlack)
                context.setTheme(R.style.AppThemePatchDark)
            }
            "auto" -> {
                when (context.resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)) {
                    Configuration.UI_MODE_NIGHT_YES -> {
                        context.setTheme(R.style.AppThemeDark_NoActionBar)
                        recent(context, R.color.colorPrimaryDark)
                        context.setTheme(R.style.AppThemePatchDark)
                    }
                    else -> {
                        context.setTheme(R.style.AppTheme_NoActionBar)
                        recent(context, R.color.colorPrimary)
                        context.setTheme(R.style.AppThemePatch)
                    }
                }
            }
            else -> {
                context.setTheme(R.style.AppTheme_NoActionBar)
                recent(context, R.color.colorPrimary)
                context.setTheme(R.style.AppThemePatch)
            }
        }
    }

    private fun recent(c: Context, color: Int) {
        val taskDescription = ActivityManager.TaskDescription(
                c.getString(R.string.app_name),
                BitmapFactory.decodeResource(c.resources, R.mipmap.ic_launcher),
                ContextCompat.getColor(c, color)
        )
        (c as Activity).setTaskDescription(taskDescription)
    }
}
