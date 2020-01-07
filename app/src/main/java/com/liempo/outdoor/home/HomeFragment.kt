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
import com.google.android.material.snackbar.Snackbar

import com.liempo.outdoor.R
import com.liempo.outdoor.SpeechRecognitionModel
import kotlinx.android.synthetic.main.home_fragment.*

class HomeFragment : Fragment() {

    private lateinit var model: HomeViewModel
    private lateinit var speech: SpeechRecognitionModel

    private lateinit var fused: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize fused
        fused = LocationServices.getFusedLocationProviderClient(requireContext())
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

            // Animate rms view, loading
            rms_view.transform()

            // Get keyword
            val keyword = model.extractKeyword(
                speech.recognizedText.value)

            // Get the last known location
            fused.lastLocation.addOnSuccessListener { location ->

            }

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

    }

}
