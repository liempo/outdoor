package com.liempo.outdoor.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beust.klaxon.Klaxon
import com.liempo.outdoor.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class HomeViewModel : ViewModel() {

    internal val place: MutableLiveData<String?> = MutableLiveData()

    internal fun extractKeyword(text: String?): String? {
        var result = text

        for (ignored in IGNORED_WORDS) {
            result = result?.replace(ignored, "")
        }

        return result
    }

    internal fun findPlacesNearby(
        keyword: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            val client = OkHttpClient()
            val url = "https://maps.googleapis.com/" +
                    "maps/api/place/findplacefromtext/" +
                    "json?inputtype=textquery&" +
                    "input=$keyword&" +
                    "locationbias=point:$lat,$lng" +
                    "&key=${BuildConfig.PlacesApiKey}"
            val request = Request.Builder().url(url).build()

            place.value =  withContext(Dispatchers.IO) {
                val raw = client.newCall(request).execute()
                val body = raw.body!!.string()

                // CLose response to avoid leaks
                raw.close()

                val response = Klaxon().
                    parse<PlaceSearchResponse>(body)!!

                return@withContext if (response.candidates.isNotEmpty())
                        response.candidates[0].place_id else null
            }
        }
    }

    data class PlaceSearchResponse(
        val candidates: List<Candidate>,
        val status: String
    )

    data class Candidate(
        val place_id: String
    )

    companion object {
        private val IGNORED_WORDS = arrayOf(
            "find", "find a", "nearby", "near me", "look for a"
        )
    }
}
