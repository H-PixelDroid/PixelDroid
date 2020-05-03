package com.h.pixeldroid.objects

import java.io.Serializable

/*
NOT DOCUMENTED, USE WITH CAUTION
 */

data class DiscoverPost(
    val type: String?, //This is probably an enum, with these values: https://github.com/pixelfed/pixelfed/blob/700c7805cecc364b68b9cfe20df00608e0f6c465/app/Status.php#L31
    val url: String?, //URL to post
    val thumb: String? //URL to thumbnail
) : Serializable