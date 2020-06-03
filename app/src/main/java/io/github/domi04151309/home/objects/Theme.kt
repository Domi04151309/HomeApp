package io.github.domi04151309.home.objects

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.preference.PreferenceManager
import androidx.core.content.ContextCompat
import io.github.domi04151309.home.R

object Theme {

    fun set(context: Context) {
        when (PreferenceManager.getDefaultSharedPreferences(context).getString("theme", "light")) {
            "light" -> {
                context.setTheme(R.style.AppTheme27)
                recent(context, R.color.colorPrimary)
            }
            "dark" -> {
                context.setTheme(R.style.AppThemeDark)
                recent(context, R.color.colorPrimaryDark)
            }
            "black" -> {
                context.setTheme(R.style.AppThemeBlack)
                recent(context, R.color.colorPrimaryBlack)
            }
            else -> {
                context.setTheme(R.style.AppTheme27)
                recent(context, R.color.colorPrimary)
            }
        }
        context.setTheme(R.style.AppThemePatch)
    }

    fun setNoActionBar(context: Context) {
        when (PreferenceManager.getDefaultSharedPreferences(context).getString("theme", "light")) {
            "light" -> {
                context.setTheme(R.style.AppTheme_NoActionBar)
                recent(context, R.color.colorPrimary)
            }
            "dark" -> {
                context.setTheme(R.style.AppThemeDark_NoActionBar)
                recent(context, R.color.colorPrimaryDark)
            }
            "black" -> {
                context.setTheme(R.style.AppThemeBlack_NoActionBar)
                recent(context, R.color.colorPrimaryBlack)
            }
            else -> {
                context.setTheme(R.style.AppTheme_NoActionBar)
                recent(context, R.color.colorPrimary)
            }
        }
        context.setTheme(R.style.AppThemePatch)
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
