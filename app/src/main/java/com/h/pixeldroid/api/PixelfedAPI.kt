package com.h.pixeldroid.api

import com.h.pixeldroid.objects.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/*
    Implements the Pixelfed API
    https://docs.pixelfed.org/technical-documentation/api-v1.html
    However, since this is mostly based on the Mastodon API, the documentation there
    will be more useful: https://docs.joinmastodon.org/
 */

interface PixelfedAPI {

    @GET("/api/v1/timelines/public")
    fun timelinePublic(
        @Query("local") local: Boolean?,
        @Query("max_id") max_id: String?,
        @Query("since_id") since_id: String?,
        @Query("min_id") min_id: String?,
        @Query("limit") limit: Int?
    ): Call<List<Status>>

    companion object {
        fun create(baseUrl: String): PixelfedAPI {
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build().create(PixelfedAPI::class.java)
        }
    }
}

