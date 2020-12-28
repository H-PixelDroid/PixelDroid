package com.h.pixeldroid.utils.api.objects

import java.io.Serializable

data class Context(
        val ancestors : List<Status>,
        val descendants : List<Status>
) : Serializable