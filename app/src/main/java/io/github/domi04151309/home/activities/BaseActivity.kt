package io.github.domi04151309.home.activities

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.domi04151309.home.R
import io.github.domi04151309.home.helpers.LocaleHelper

abstract class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Update configuration before creating the activity
        LocaleHelper.updateActivityConfiguration(this)
        
        super.onCreate(savedInstanceState)
        if (resources.configuration.uiMode.and(
                Configuration.UI_MODE_NIGHT_MASK,
            ) != Configuration.UI_MODE_NIGHT_YES
        ) {
            setTheme(R.style.LightStatusBarOverlay)
        }

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, 0)
            windowInsets
        }
    }

    fun applyBottomInsetPadding(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                insets.bottom,
            )
            windowInsets
        }
    }

    fun applyBottomInsetMargin(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            (v.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = insets.bottom
            windowInsets
        }
    }
}
