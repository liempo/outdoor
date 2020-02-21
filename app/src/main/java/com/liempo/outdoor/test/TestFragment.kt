@file:Suppress("DEPRECATION")

package com.liempo.outdoor.test

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.liempo.outdoor.R
import com.liempo.outdoor.SpeechRecognitionModel
import com.liempo.outdoor.detection.DetectorActivity
import com.mapbox.geojson.Point
import kotlinx.android.synthetic.main.fragment_test.*
import kotlinx.android.synthetic.main.fragment_test.detected_text

class TestFragment : Fragment() {

    private val speech: SpeechRecognitionModel by viewModels()
    private val model: TestViewModel by viewModels()
    private lateinit var progress: ProgressDialog
    private lateinit var fused: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fused = LocationServices.getFusedLocationProviderClient(requireContext())
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.fragment_test,
        container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Setup speech model
        speech.recognizedText.observe(viewLifecycleOwner, Observer {
            detected_text.text = it
        })

        speech.error.observe(viewLifecycleOwner, Observer {
            Toast.makeText(context, "Error: $it",
                Toast.LENGTH_SHORT).show()
        })

        model.routeJson.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer

            progress.show()
            findNavController().navigate(
                TestFragmentDirections.startTestNavigation(it))
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        detection_card.setOnClickListener {
            startActivity(Intent(requireActivity(),
                DetectorActivity::class.java))
        }
        progress = ProgressDialog(context).apply {
            setMessage("Loading navigation")
        }

        navigation_card.setOnClickListener {

            progress.show()

            fused.lastLocation.addOnSuccessListener { loc ->
                model.getBestRoute(
                        Point.fromLngLat(loc.longitude, loc.latitude),
                        Point.fromLngLat(121.157478,14.191168)
                )
            }

//            FirebaseFirestore.getInstance().collection("profile")
//                .document(FirebaseAuth.getInstance().uid!!).get()
//                .addOnSuccessListener { snapshot ->
//                    val home = snapshot["home"] as GeoPoint
//
//                    fused.lastLocation.addOnSuccessListener { loc ->
//                        model.getBestRoute(
//                            Point.fromLngLat(loc.longitude, loc.latitude),
//                            Point.fromLngLat(121.157478,14.191168)
//                        )
//                    }
//                }
//                .addOnFailureListener {
//                    Toast.makeText(context,
//                            "Error getting home location",
//                            Toast.LENGTH_SHORT).show()
//                }
        }

        speech_recognition_card.setOnClickListener {
            if (speech.isListening.value == true) {
                speech.stopListening()
            } else {
                speech.startListening()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (progress.isShowing) progress.dismiss()
    }

}
