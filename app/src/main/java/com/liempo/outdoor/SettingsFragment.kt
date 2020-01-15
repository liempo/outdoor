package com.liempo.outdoor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.places.picker.PlacePicker
import com.mapbox.mapboxsdk.plugins.places.picker.model.PlacePickerOptions
import timber.log.Timber

class SettingsFragment : PreferenceFragmentCompat() {

    // Will be used to check if user is logged in
    private lateinit var auth: FirebaseAuth

    // Firestore, to saved initialized data
    private lateinit var db: FirebaseFirestore

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Initialize firebase auth
        auth = FirebaseAuth.getInstance()

        // Initialize firebase firestore
        db = FirebaseFirestore.getInstance()

        // Initialize place picker intent
        val options = PlacePickerOptions.builder()
            // Look at this fucking shit, who has typos in their code?
            .statingCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(14.191168,121.157478))
                    .zoom(10.0).build())
            .build()
        val picker = PlacePicker.IntentBuilder()
            .accessToken(BuildConfig.MapboxApiKey)
            .placeOptions(options)
            .build(activity)

        findPreference<Preference>("pref_home")?.setOnPreferenceClickListener {
            startActivityForResult(picker, RC_PLACE_PICKER)

            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) when (requestCode) {

            RC_PLACE_PICKER -> {
                val place = PlacePicker.getPlace(data)!!
                val location = GeoPoint(
                    place.center()!!.latitude(),
                    place.center()!!.longitude())

                db.collection("home")
                    .document(auth.currentUser!!.uid)
                    .set(hashMapOf("location" to location))
                    .addOnSuccessListener {
                        Timber.i("Successfully updated database.")
                    }
            }
        }
    }

    companion object {
        private const val RC_PLACE_PICKER = 15145
    }
}