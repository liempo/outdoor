package com.liempo.outdoor.splash


import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber

import com.liempo.outdoor.R

class SplashFragment : Fragment() {

    // Will be used to check if user is logged in
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize firebase auth
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_splash,
                    container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Check if not logged in
        if (auth.currentUser == null) {
            // List of log in providers (log in with phone only)
            val providers = arrayListOf(
                    AuthUI.IdpConfig.PhoneBuilder().build()
            )

            // Start Firebase UI authentication
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN)
        }

        // Ask for permissions for the whole app if not granted
        if (APP_PERMISSIONS.all { activity?.checkSelfPermission(it) ==
                    PackageManager.PERMISSION_GRANTED }.not()) {
            requestPermissions(APP_PERMISSIONS, RC_APP_PERMISSIONS)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            Timber.d("User logged in: ${auth.currentUser?.phoneNumber}")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == RC_APP_PERMISSIONS && grantResults.
                        any { it != PackageManager.PERMISSION_GRANTED }) {
            AlertDialog.Builder(activity)
                    .setMessage(R.string.error_missing_permissions)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        activity?.finish()
                    }.show()
        }
    }

    companion object {
        private const val RC_SIGN_IN = 2934

        private const val RC_APP_PERMISSIONS = 5123
        private val APP_PERMISSIONS = arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CAMERA
        )
    }
}
