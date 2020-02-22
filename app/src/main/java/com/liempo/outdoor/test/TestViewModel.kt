package com.liempo.outdoor.test

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liempo.outdoor.BuildConfig
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TestViewModel: ViewModel() {

    internal val routeJson: MutableLiveData<String?> = MutableLiveData()

    internal fun getBestRoute(origin: Point, destination: Point) {
        viewModelScope.launch {

            val builder = MapboxDirections.builder()
                .accessToken(BuildConfig.MapboxApiKey)
                .origin(origin)
                .destination(destination)
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .voiceInstructions(true)
                .bannerInstructions(true)
                // I don' know what the
                // fuck does this param do
                .steps(true)

            routeJson.value = withContext(Dispatchers.IO) {
                // first route is the best route
                val routes = builder.build().executeCall().body()?.routes()

                // return best route, if route is empty return null
                return@withContext if (routes.isNullOrEmpty().not())
                    routes!![0].toJson() else null
            }
        }
    }
}