package com.h.pixeldroid.utils

import android.content.SharedPreferences
import com.h.pixeldroid.db.PostEntity
import com.h.pixeldroid.objects.Status
import java.util.*

class PostEntityConverter {

    companion object {

        /**
        Converts a Status to a PostEntity. Needs the right SharedPreferences to know the domain
         */
        fun statusToPostEntity(status: Status): PostEntity {

            return PostEntity(
                uid = 0, // auto-generate the key
                domain = "",
                username = status.account.username,
                displayName = status.account.display_name,
                description = status.content,
                accountID = status.account.id,
                nbLikes = status.favourites_count,
                nbShares = status.reblogs_count,
                ImageURL = status.media_attachments[0].url,
                profileImgUrl = status.account.avatar,
                date = Calendar.getInstance().time
            )
        }
    }

}