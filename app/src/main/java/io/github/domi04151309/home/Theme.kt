package io.github.domi04151309.home

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.BitmapFactory
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat

object Theme {

    fun set(context: Context) {
        when (PreferenceManager.getDefaultSharedPreferences(context).getString("theme", "light")) {
            "light" -> {
                context.setTheme(R.style.AppTheme)
                recent(context, R.color.colorPrimary)
            }
            "dark" -> {
                context.setTheme(R.style.AppTheme_Dark)
                recent(context, R.color.colorPrimaryDark)
            }
            "black" -> {
                context.setTheme(R.style.AppTheme_Black)
                recent(context, R.color.colorPrimaryBlack)
            }
            else -> {
                context.setTheme(R.style.AppTheme)
                recent(context, R.color.colorPrimary)
            }
        }
    }

    fun setNoActionBar(context: Context) {
        when (PreferenceManager.getDefaultSharedPreferences(context).getString("theme", "light")) {
            "light" -> {
                context.setTheme(R.style.AppTheme_NoActionBar)
                recent(context, R.color.colorPrimary)
            }
            "dark" -> {
                context.setTheme(R.style.AppTheme_Dark_NoActionBar)
                recent(context, R.color.colorPrimaryDark)
            }
            "black" -> {
                context.setTheme(R.style.AppTheme_Black_NoActionBar)
                recent(context, R.color.colorPrimaryBlack)
            }
            else -> {
                context.setTheme(R.style.AppTheme_NoActionBar)
                recent(context, R.color.colorPrimary)
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
