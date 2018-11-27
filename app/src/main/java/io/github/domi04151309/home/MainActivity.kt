package io.github.domi04151309.home

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*



class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
        nav_view.setCheckedItem(R.id.nav_devices)

        val listView = findViewById<View>(R.id.listView) as ListView
        val titles = arrayOf("One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten")
        val summaries = arrayOf("Online", "Online", "Online", "Online", "Online", "Online", "Online", "Offline", "Offline", "Offline")

        val adapter = ListAdapter(this, titles, summaries)
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_devices -> {
                // Handle the action
            }
            R.id.nav_wiki -> {
                val uri = Uri.parse("https://github.com/Domi04151309/home/wiki")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            R.id.nav_settings -> {

            }
            R.id.nav_source -> {
                val uri = Uri.parse("https://github.com/Domi04151309/home")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            R.id.nav_google_home -> {
                val intent = Intent()
                intent.component = ComponentName("com.google.android.apps.chromecast.app", "com.google.android.apps.chromecast.app.DiscoveryActivity")
                startActivity(intent);
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
