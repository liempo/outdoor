package com.liempo.outdoor.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.liempo.outdoor.BuildConfig
import com.liempo.outdoor.R
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.places.picker.PlacePicker
import com.mapbox.mapboxsdk.plugins.places.picker.model.PlacePickerOptions
import timber.log.Timber

class ProfileFragment : PreferenceFragmentCompat() {

    // Will be used to check if user is logged in
    private lateinit var auth: FirebaseAuth

    // Firestore, to saved initialized data
    private lateinit var store: FirebaseFirestore

    // Storage object for firestore
    private lateinit var storage: StorageReference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        // Initialize firebase auth
        auth = FirebaseAuth.getInstance()

        // Initialize firebase firestore
        store = FirebaseFirestore.getInstance()

        // Initialize firebase cloud storage
        storage = FirebaseStorage.getInstance().reference

//        profilePicture.setOnPreferenceClickListener {
//            startActivityForResult(Intent(ACTION_PICK)
//                .apply { type = "image/*" }, RC_IMAGE_PICKER)
//            true
//        }

        storage.child("profile_pic/${auth.uid}")
            .stream.addOnSuccessListener {
            it.storage.downloadUrl.addOnSuccessListener { uri ->
                ProfileImagePreference(requireContext(), uri)


            }
        }
    }

    private fun initializePreferences(rootKey: String) {
        setPreferencesFromResource(R.xml.profile_preferences, rootKey)

        findPreference<Preference>("pref_guardian_number")?.apply {
            summary = FirebaseAuth.getInstance().currentUser?.phoneNumber
            setOnPreferenceClickListener {
                auth.signOut()
                findNavController().navigate(
                    ProfileFragmentDirections.logout()
                )

                true
            }
        }

        findPreference<Preference>("pref_home")?.setOnPreferenceClickListener {
            startActivityForResult(getPlacePickerIntent(), RC_PLACE_PICKER)

            true
        }

        findPreference<Preference>("pref_logout")?.setOnPreferenceClickListener {
            auth.signOut()
            findNavController().navigate(
                ProfileFragmentDirections.logout()
            )

            true
        }
    }

    private fun getPlacePickerIntent(): Intent {
        // Initialize place picker intent
        val options = PlacePickerOptions.builder()
            // Look at this fucking shit, who has typos in their code?
            .statingCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(14.191168,121.157478))
                    .zoom(10.0).build())
            .build()
        return PlacePicker.IntentBuilder()
            .accessToken(BuildConfig.MapboxApiKey)
            .placeOptions(options)
            .build(activity)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK &&
            data != null && data.data != null)
            when (requestCode) {

                RC_PLACE_PICKER -> {
                    val place = PlacePicker.getPlace(data)!!
                    val location = GeoPoint(
                        place.center()!!.latitude(),
                        place.center()!!.longitude())

                    store.collection("home")
                        .document(auth.currentUser!!.uid)
                        .set(hashMapOf("location" to location))
                        .addOnSuccessListener {
                            Timber.i("Successfully updated database.")
                        }
                }

                RC_IMAGE_PICKER -> {
                    data.data!!
                }
            }
    }

    companion object {
        private const val RC_PLACE_PICKER = 15145
        private const val RC_IMAGE_PICKER = 17453
    }
}