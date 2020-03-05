package com.h.pixeldroid.objects

import java.io.Serializable

data class Emoji(
    //Required attributes
    val shortcode: String,
    val url: String, //URL
    val static_url: String, //URL
    val visible_in_picker: Boolean,
    //Optional attributes
    val category: String? = null
) : Serializable

