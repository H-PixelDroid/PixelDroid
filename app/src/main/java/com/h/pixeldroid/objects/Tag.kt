package com.h.pixeldroid.objects

data class Tag(
    //Base attributes
    val name: String,
    val url: String,
    //Optional attributes
    val history: List<History>? = emptyList()
)

