package com.liempo.outdoor.navigation

import android.content.Intent
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import androidx.navigation.navArgs
import com.google.firebase.auth.FirebaseAuth
import com.liempo.outdoor.R
import com.liempo.outdoor.detection.DetectorActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import kotlinx.android.synthetic.main.activity_navigation.*
import safety.com.br.android_shake_detector.core.ShakeDetector
import safety.com.br.android_shake_detector.core.ShakeOptions
import timber.log.Timber

class NavigationActivity : AppCompatActivity(),
    NavigationListener, ProgressChangeListener, OffRouteListener {

    private val args: NavigationActivityArgs by navArgs()

    private var emergency = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        // Initialize shake detector
        val shakeOptions = ShakeOptions()
            .interval(1000)
            .shakeCount(5)
            .sensibility(2.0f)
        ShakeDetector(shakeOptions).start(this) {
            emergency = true
        }

        // Initialize navigation view
        nav_view.onCreate(savedInstanceState)
        nav_view.initialize {
            // Parse json to object (DirectionsRoute)
            val directions = DirectionsRoute.fromJson(args.routeJson)

            // Configure options
            val builder = NavigationViewOptions.builder()
                .directionsRoute(directions)
                .navigationListener(this)
                .progressChangeListener(this)

            nav_view.startNavigation(builder.build())
        }

        val detector = GestureDetector(
            this, object : SimpleOnGestureListener() {

            override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
                startActivity(Intent(this@NavigationActivity,
                    DetectorActivity::class.java))
                return super.onDoubleTapEvent(e)
            }
        })

        nav_view.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
        }

    }

    override fun onNavigationFinished() {
        nav_view.stopNavigation()
        onBackPressed()
    }

    override fun onNavigationRunning() {}

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

    override fun onProgressChange(location: Location?, routeProgress: RouteProgress?) {
        Timber.d("Emergency: $emergency")
        if (emergency) {
            SmsManager.getDefault().sendTextMessage(
                FirebaseAuth.getInstance()
                    .currentUser?.phoneNumber, null,
                "Emergency detected in " +
                        "https://www.google.com/maps/search/" +
                        "?api=1&query=${location?.latitude},${location?.longitude}>.",
                null, null
            )
            emergency = false
        }
    }

    override fun userOffRoute(location: Location?) {
        SmsManager.getDefault().sendTextMessage(
            FirebaseAuth.getInstance()
                .currentUser?.phoneNumber, null,
            "User is off the route" +
                    "https://www.google.com/maps/search/" +
                    "?api=1&query=${location?.latitude},${location?.longitude}>.",
            null, null
        )
    }

}
