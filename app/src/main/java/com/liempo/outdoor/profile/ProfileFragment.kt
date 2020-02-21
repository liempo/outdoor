package com.liempo.outdoor.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.firebase.ui.auth.AuthUI
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
import kotlinx.android.synthetic.main.fragment_profile.*
import timber.log.Timber

class ProfileFragment : Fragment() {

    // Will be used to check if user is logged in
    private lateinit var auth: FirebaseAuth

    // Firestore, to saved initialized data
    private lateinit var store: FirebaseFirestore

    // Storage object for firestore
    private lateinit var storage: StorageReference

    private var editable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize firebase auth
        auth = FirebaseAuth.getInstance()

        // Initialize firebase firestore
        store = FirebaseFirestore.getInstance()

        // Initialize firebase cloud storage
        storage = FirebaseStorage.getInstance().reference
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.fragment_profile,
        container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storage.child("profile_pic")
            .child(auth.uid!!)
            .downloadUrl.addOnSuccessListener {
            Glide.with(profile_image)
                .load(it)
                .into(profile_image)
        }

        store.collection("profile")
            .document(auth.currentUser!!.uid)
            .get().addOnSuccessListener {
                user_name_input.setText(it["user_name"].toString())
                guardian_name_input.setText(it["guardian_name"].toString())
            }

        change_number_button.text = auth.currentUser!!.phoneNumber

        profile_image.setOnClickListener {
            startImagePicker()
        }

        edit_button.setOnClickListener {
            editable = editable.not()
            Timber.d("isEditable = $editable")
            isEditEnabled(editable)
        }

        setup_home_button.setOnClickListener {
            startPlacePicker()
        }

        change_number_button.setOnClickListener {
            startFirebaseAuth()
        }

        logout_card.setOnClickListener {
            Timber.d("CurrentUser: ${auth.currentUser}")

            auth.signOut()
            findNavController().navigate(
                ProfileFragmentDirections.logout())
        }
    }

    private fun isEditEnabled(value: Boolean) {
        // Update firebase elements
        val userName = user_name_input.text.toString()
        val guardianName = guardian_name_input.text.toString()

        store.collection("profile")
            .document(auth.currentUser!!.uid)
            .set(hashMapOf("user_name" to userName,
                "guardian_name" to guardianName))
            .addOnSuccessListener {
                Toast.makeText(context, "Updated profile",
                    Toast.LENGTH_SHORT).show()
            }

        user_name_input.isEnabled = value
        guardian_name_input.isEnabled = value
        change_number_button.isEnabled = value

        edit_button.text = if (value)
            getString(R.string.action_edit_profile)
        else getString(R.string.action_save_profile)

    }

    private fun startPlacePicker() {
        // Initialize place picker intent
        val options = PlacePickerOptions.builder()
            // Look at this fucking shit, who has typos in their code?
            .statingCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(14.191168,121.157478))
                    .zoom(10.0).build())
            .build()
        val intent = PlacePicker.IntentBuilder()
            .accessToken(BuildConfig.MapboxApiKey)
            .placeOptions(options)
            .build(activity)
        startActivityForResult(intent, RC_IMAGE_PICKER)
    }

    private fun startImagePicker() {
        val intent = Intent().apply {
            action = Intent.ACTION_GET_CONTENT
            type = "image/*"
        }

        startActivityForResult(intent, RC_IMAGE_PICKER)
    }

    private fun startFirebaseAuth() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.PhoneBuilder().build()
        )

        // Start Firebase UI authentication
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
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
                    storage.child("profile_pic/${auth.uid}")
                        .putFile(data.data!!)
                        .addOnSuccessListener {
                            Glide.with(profile_image)
                                .load(data.data!!)
                                .into(profile_image)
                        }
                }
            }
    }

    companion object {
        private const val RC_SIGN_IN = 12311
        private const val RC_PLACE_PICKER = 15145
        private const val RC_IMAGE_PICKER = 17453
    }
}