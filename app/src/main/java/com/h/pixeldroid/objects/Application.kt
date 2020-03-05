package com.h.pixeldroid.objects

import java.io.Serializable

data class Application (
    //Required attributes
    val name: String,
    //Optional attributes
    val website: String? = null,
    val vapid_key: String? = null
) : Serializable

