package io.github.domi04151309.home

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class SplashScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this@SplashScreen, MainActivity::class.java))
        finish()
    }
}
