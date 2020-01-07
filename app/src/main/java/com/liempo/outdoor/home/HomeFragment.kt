package com.liempo.outdoor.home

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.snackbar.Snackbar

import com.liempo.outdoor.R
import com.liempo.outdoor.SpeechRecognitionModel
import com.mapbox.geojson.Point
import kotlinx.android.synthetic.main.home_fragment.*
import timber.log.Timber

class HomeFragment : Fragment() {

    private lateinit var model: HomeViewModel
    private lateinit var speech: SpeechRecognitionModel

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var places: PlacesClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize fused and places sdk
        fused = LocationServices.getFusedLocationProviderClient(requireContext())
        places = Places.createClient(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.home_fragment,
        container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        model = ViewModelProviders.of(this)
            .get(HomeViewModel::class.java)
        speech = ViewModelProviders.of(this)
            .get(SpeechRecognitionModel::class.java)

        // Setup speech model
        speech.recognizedText.observe(this, Observer {
            detected_text.text = it
        })

        speech.error.observe(this, Observer {
            Snackbar.make(bar, it, Snackbar.LENGTH_SHORT).show()
        })

        speech.rmsValue.observe(this, Observer {
            rms_view.setRms(it)
        })

        speech.isListening.observe(this, Observer {
            // Ignore if still listening
            if (it) return@Observer
            Timber.i("triggered")

            // Animate rms view, loading
            rms_view.transform()

            // Get keyword else exit
            val keyword = model.extractKeyword(
                speech.recognizedText.value)
                ?: return@Observer

            // Get the last known location
            fused.lastLocation.addOnSuccessListener { loc ->
                model.findPlacesNearby(keyword,
                    loc.latitude, loc.longitude)
            }
        })

        model.place.observe(this, Observer {
            if (it == null) return@Observer

            Timber.i("PlaceId: $it")

            // Get place LatLng
            val request = FetchPlaceRequest
                .builder(it, listOf(Place.Field.LAT_LNG)).build()
            places.fetchPlace(request).addOnSuccessListener { place ->
                val dest = Point.fromLngLat(
                    place.place.latLng!!.longitude,
                    place.place.latLng!!.latitude)

                fused.lastLocation.addOnSuccessListener { loc ->
                    val origin = Point.fromLngLat(
                        loc.longitude, loc.latitude
                    )

                    model.getBestRoute(origin, dest)
                }
            }
        })

        model.routeJson.observe(this, Observer {
            // TODO start
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup rms view
        rms_view.setColors(intArrayOf(
            getColor(requireContext(), R.color.colorPrimary),
            getColor(requireContext(), R.color.colorAccent),
            getColor(requireContext(), R.color.colorPrimaryDark),
            getColor(requireContext(), R.color.colorPrimary),
            getColor(requireContext(), R.color.colorAccent)
        )); rms_view.play()

        fab.setOnClickListener {
            if (speech.isListening.value == true) {
                speech.stopListening()
            } else speech.startListening()
        }

    }

}
