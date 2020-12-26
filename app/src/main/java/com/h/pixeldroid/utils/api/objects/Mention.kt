package com.h.pixeldroid.utils.api.objects

import java.io.Serializable

data class Mention(
    //Mentioned user
    val id: String,
    val username : String,
    val acct : String, //URI of mentioned user (username if local, else username@domain)
    val url  : String //URL of mentioned user's profile
) : Serializable