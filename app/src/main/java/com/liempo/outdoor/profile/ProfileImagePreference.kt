package com.liempo.outdoor.profile

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.widget.ImageView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.bumptech.glide.Glide
import com.liempo.outdoor.R

class ProfileImagePreference(context: Context,
                             private val uri: Uri,
                             attrs: AttributeSet? = null) :
    Preference(context, attrs) {

    init {
        this.layoutResource = R.layout.layout_profile
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        if (holder == null) return

        val image = holder.findViewById(R.id.profile_image) as ImageView
        Glide.with(image)
            .load(uri)
            .into(image)
    }
}