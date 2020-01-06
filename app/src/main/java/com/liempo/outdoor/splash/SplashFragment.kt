package com.liempo.outdoor.splash


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth

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
                    REQUEST_SIGN_IN)
        }
    }

    companion object {
        private const val REQUEST_SIGN_IN = 2934
    }
}
