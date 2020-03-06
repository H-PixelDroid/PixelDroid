package com.h.pixeldroid.objects

import java.io.Serializable

data class Card(
    //Required attributes
    val url: String, //URL
    val title: String,
    val description: String,
    val type: CardType,
    //Optional attributes
    val author_name: String? = null,
    val author_url: String? = null, //URL
    val provider_name: String? = null,
    val provider_url: String? = null, //URL
    val html: String? = null, //HTML
    val width: Int? = null,
    val height: Int? = null,
    val image: String? = null, //URL
    val embed_url: String? = null //URL
) : Serializable {
    enum class CardType {
        link, photo, video, rich
    }
}
