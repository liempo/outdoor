package com.liempo.outdoor.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect.*
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.QUEUE_ADD
import android.telephony.SmsManager
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.liempo.outdoor.R
import com.liempo.outdoor.SpeechRecognitionModel
import com.liempo.outdoor.detection.DetectorActivity
import com.mapbox.geojson.Point
import kotlinx.android.synthetic.main.fragment_home.*
import safety.com.br.android_shake_detector.core.ShakeDetector
import safety.com.br.android_shake_detector.core.ShakeOptions
import timber.log.Timber
import java.util.*

class HomeFragment : Fragment() {

    private val model: HomeViewModel by viewModels()
    private val speech: SpeechRecognitionModel by viewModels()

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var places: PlacesClient

    // Will be used to please horny women
    private lateinit var vibrator: Vibrator

    // TTS for place found
    private lateinit var tts: TextToSpeech

    // Shake it off, shake it off who who who
    private lateinit var shake: ShakeDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize fused and places sdk
        fused = LocationServices.getFusedLocationProviderClient(requireContext())
        places = Places.createClient(requireContext())

        // Initialize vibrator 2000
        vibrator = context?.getSystemService(
            Context.VIBRATOR_SERVICE) as Vibrator

        // Initialize text to speech
        tts = TextToSpeech(context) {
            if (it != TextToSpeech.ERROR) {
                tts.language = Locale.US
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home,
        container, false)

    @SuppressLint("DefaultLocale")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Set up back button not working
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                requireActivity().finish()
            }
        }

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, callback)

        // Setup speech model
        speech.recognizedText.observe(viewLifecycleOwner, Observer {
            detected_text.text = it
        })

        speech.error.observe(viewLifecycleOwner, Observer {
            Toast.makeText(context, "Error: $it",
                Toast.LENGTH_SHORT).show()
        })

        speech.rmsValue.observe(viewLifecycleOwner, Observer {
            rms_view.setRms(it)
        })

        speech.isListening.observe(viewLifecycleOwner, Observer {
            // Ignore if still listening
            if (it) return@Observer
            Timber.i("triggered")

            // Animate rms view, loading
            rms_view.transform()

            // Get keyword else exit
            val text = speech.recognizedText.value
            if (text?.toLowerCase() == "go home") {
                FirebaseFirestore.getInstance().collection("profile")
                    .document(FirebaseAuth.getInstance().uid!!).get()
                    .addOnSuccessListener { snapshot ->
                        val home = snapshot["home"] as GeoPoint

                        fused.lastLocation.addOnSuccessListener { loc ->
                            model.getBestRoute(
                                Point.fromLngLat(loc.longitude, loc.latitude),
                                Point.fromLngLat(home.longitude, home.latitude)
                            )
                        }

                        rms_view.startIdleInterpolation()
                    }
            } else {
                val keyword = model.extractKeyword(text)
                    ?: return@Observer

                // Get the last known location
                fused.lastLocation.addOnSuccessListener { loc ->
                    model.findPlacesNearby(
                        keyword,
                        loc.latitude, loc.longitude
                    )

                    rms_view.startIdleInterpolation()
                }
            }
        })

        model.place.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer

            // Get place LatLng
            val request = FetchPlaceRequest
                .builder(it, listOf(
                    Place.Field.NAME,
                    Place.Field.LAT_LNG)
                ).build()
            places.fetchPlace(request).addOnSuccessListener { place ->
                val placeName = place.place.name!!
                detected_text.text = placeName
                tts.speak(placeName, QUEUE_ADD, null, null)

                val dest = Point.fromLngLat(
                    place.place.latLng!!.longitude,
                    place.place.latLng!!.latitude)

                fused.lastLocation.addOnSuccessListener { loc ->
                    val origin = Point.fromLngLat(
                        loc.longitude, loc.latitude
                    )

                    model.getBestRoute(origin, dest)
                }
            }.addOnFailureListener { e ->
                Timber.e(e, "Error Fetching $it")
                if (e is ApiException) {
                    Snackbar.make(rms_cardview,
                        "Exceed the Places API Quota.",
                        Snackbar.LENGTH_SHORT)
                        .show()
                }

                rms_view.stop(); rms_view.play()
            }
        })

        model.routeJson.observe(viewLifecycleOwner, Observer {
            Timber.i("Route: $it")

            if (it == null) return@Observer
            findNavController().navigate(
                HomeFragmentDirections.startNavigation(it)
            )
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

        rms_view.setOnClickListener {
            if (speech.isListening.value == true) {
                speech.stopListening()
            } else {
                speech.startListening()
                rms_view.startRmsInterpolation()
            }
        }

        profile_cardview.setOnClickListener {
            findNavController().navigate(
                HomeFragmentDirections.openSettings())
        }

        settings_cardview.setOnClickListener {
            findNavController().navigate(
                HomeFragmentDirections.openTest())
        }

        val detector = GestureDetector(
            requireContext(), object : SimpleOnGestureListener() {

                override fun onDoubleTap(e: MotionEvent?): Boolean {
                    vibrator.vibrate(createWaveform(
                        longArrayOf(50, 50), -1))

                    // Change command from here
                    speech.recognizedText.value = "Go home"

                    // Trigger route generation
                    speech.isListening.value = false
                    return super.onDoubleTap(e)
                }

                override fun onShowPress(e: MotionEvent?) {
                    vibrator.vibrate(createOneShot(
                        50, DEFAULT_AMPLITUDE))
                }

                override fun onSingleTapUp(e: MotionEvent?): Boolean {
                    vibrator.vibrate(createOneShot(
                        100, DEFAULT_AMPLITUDE))
                    if (speech.isListening.value == true) {
                        speech.stopListening()
                    } else {
                        speech.startListening()
                        rms_view.startRmsInterpolation()
                    }
                    return true
                }

                override fun onLongPress(e: MotionEvent?) {
                    vibrator.vibrate(createOneShot(
                        300, DEFAULT_AMPLITUDE))
                    startActivity(Intent(requireActivity(),
                        DetectorActivity::class.java))
                }

            })

        gesture_cardview.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
        }

        // Initialize shake detector
        val shakeOptions = ShakeOptions()
            .interval(1000)
            .shakeCount(5)
            .sensibility(2.0f)
        shake = ShakeDetector(shakeOptions).start(context) {
            fused.lastLocation.addOnSuccessListener {
                Toast.makeText(context,
                    "Sending notifcation to guardian",
                    Toast.LENGTH_LONG).show()
                SmsManager.getDefault().sendTextMessage(
                    FirebaseAuth.getInstance()
                        .currentUser?.phoneNumber, null,
                    "Emergency detected in " +
                            "https://www.google.com/maps/search/" +
                            "?api=1&query=${it.latitude}," +
                            "${it.longitude}>.",
                    null, null
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Prevent memory leaks
        tts.stop(); tts.shutdown()
        shake.destroy(context)
    }

}
