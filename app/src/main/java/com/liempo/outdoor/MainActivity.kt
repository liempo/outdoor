package com.liempo.outdoor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mapbox.mapboxsdk.Mapbox
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Mapbox for Mapbox stuff
        Mapbox.getInstance(this, BuildConfig.MapboxApiKey)

        // Plant a debug Tree here
        Timber.plant(Timber.DebugTree())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
