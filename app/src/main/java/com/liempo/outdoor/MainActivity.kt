package com.liempo.outdoor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.libraries.places.api.Places
import com.mapbox.mapboxsdk.Mapbox
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Mapbox for Mapbox stuff
        Mapbox.getInstance(this, BuildConfig.MapboxApiKey)

        // Plant a debug Tree here
        Timber.plant(Timber.DebugTree())

        // Initialize Places SDK
        Places.initialize(applicationContext, BuildConfig.PlacesApiKey)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
