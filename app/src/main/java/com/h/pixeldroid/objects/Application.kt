package com.h.pixeldroid.objects

import java.io.Serializable

data class Application (
    //Required attributes
    val name: String,
    //Optional attributes
    val website: String? = null,
    val vapid_key: String? = null,
    //Client Attributes
    val client_id: String? = null,
    val client_secret: String? = null
) : Serializable

