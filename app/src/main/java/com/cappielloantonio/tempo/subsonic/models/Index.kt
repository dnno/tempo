package com.cappielloantonio.tempo.subsonic.models

import com.google.gson.annotations.SerializedName

class Index {
    @SerializedName("artist")
    var artists: List<Artist>? = null
    var name: String? = null
}