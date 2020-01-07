package com.liempo.outdoor.home

import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    internal fun extractKeyword(text: String?): String? {
        var result = text

        for (ignored in IGNORED_WORDS) {
            result = result?.replace(ignored, "")
        }

        return result
    }

    companion object {
        private val IGNORED_WORDS = arrayOf(
            "find", "find a", "nearby", "near me", "look for a"
        )
    }
}
