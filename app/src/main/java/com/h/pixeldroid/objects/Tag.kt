package com.h.pixeldroid.objects

import java.io.Serializable

data class Tag(
    //Base attributes
    val name: String,
    val url: String,
    //Optional attributes
    val history: List<History>? = emptyList()
) : Serializable