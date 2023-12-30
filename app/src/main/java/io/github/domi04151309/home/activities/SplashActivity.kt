package io.github.domi04151309.home.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }
}
