package com.h.pixeldroid.objects

import java.io.Serializable

data class Results(
    val accounts : List<Account>,
    val statuses : List<Status>,
    val hashtags: List<Tag>
) : Serializable {

    enum class SearchType: Serializable{
        accounts, hashtags, statuses
    }
}