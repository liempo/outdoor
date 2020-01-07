package com.liempo.outdoor.navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.navArgs
import com.liempo.outdoor.R
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
import kotlinx.android.synthetic.main.activity_navigation.*

class NavigationActivity : AppCompatActivity(), NavigationListener {

    private val args: NavigationActivityArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        // Initialize navigation view
        nav_view.onCreate(savedInstanceState)
        nav_view.initialize {
            // Parse json to object (DirectionsRoute)
            val directions = DirectionsRoute.fromJson(args.routeJson)

            // Configure options
            val builder = NavigationViewOptions.builder()
                .directionsRoute(directions)
                .navigationListener(this)

            nav_view.startNavigation(builder.build())
        }
    }

    override fun onNavigationFinished() {
        nav_view.stopNavigation()
        onBackPressed()
    }

    override fun onNavigationRunning() {

    }

    override fun onCancelNavigation() {
        nav_view.stopNavigation()
        onBackPressed()
    }

    override fun onStart() {
        super.onStart()
        nav_view.onStart()
    }

    override fun onResume() {
        super.onResume()
        nav_view.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        nav_view.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        nav_view.onPause()
    }

    override fun onStop() {
        super.onStop()
        nav_view.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        nav_view.onLowMemory()
    }

}
