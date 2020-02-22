package com.liempo.outdoor.navigation

import android.content.Intent
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import android.view.MotionEvent
import android.widget.Toast
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
import com.github.pwittchen.gesture.library.Gesture
import com.github.pwittchen.gesture.library.GestureListener
import safety.com.br.android_shake_detector.core.ShakeDetector
import safety.com.br.android_shake_detector.core.ShakeOptions
import timber.log.Timber

class NavigationActivity : AppCompatActivity(),
    NavigationListener, ProgressChangeListener, OffRouteListener, GestureListener {

    private val args: NavigationActivityArgs by navArgs()
    private lateinit var shake: ShakeDetector

    private var emergency = false

    private lateinit var gesture: Gesture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        // Initialize shake detector
        val shakeOptions = ShakeOptions()
            .interval(1000)
            .shakeCount(5)
            .sensibility(2.0f)
        shake = ShakeDetector(shakeOptions).start(this) {
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

        // Setup gesture
        gesture = Gesture().apply {
            addListener(this@NavigationActivity)
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

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        gesture.dispatchTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

    override fun onDrag(motionEvent: MotionEvent?) {}

    override fun onTap(motionEvent: MotionEvent?) {
        startActivity(Intent(this,
            DetectorActivity::class.java))
    }

    override fun onPress(motionEvent: MotionEvent?) {}

    override fun onRelease(motionEvent: MotionEvent?) {}

    override fun onMove(motionEvent: MotionEvent?) {}

    override fun onLongPress(motionEvent: MotionEvent?) {}

    override fun onMultiTap(motionEvent: MotionEvent?, clicks: Int) {}

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
        Toast.makeText(this,
            "Sending notifcation to guardian",
            Toast.LENGTH_LONG).show()

        SmsManager.getDefault().sendTextMessage(
            FirebaseAuth.getInstance()
                .currentUser?.phoneNumber, null,
            "User is off the route " +
                    "https://www.google.com/maps/search/" +
                    "?api=1&query=${location?.latitude},${location?.longitude}>.",
            null, null
        )
    }

    override fun onDestroy() {
        shake.destroy(this)
        super.onDestroy()
    }

}
