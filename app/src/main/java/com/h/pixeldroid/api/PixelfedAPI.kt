package com.h.pixeldroid.api

import android.util.Log
import com.h.pixeldroid.objects.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Field
/*
    Implements the Pixelfed API
    https://docs.pixelfed.org/technical-documentation/api-v1.html
    However, since this is mostly based on the Mastodon API, the documentation there
    will be more useful: https://docs.joinmastodon.org/
 */

interface PixelfedAPI {

    @FormUrlEncoded
    @POST("/api/v1/apps")
    fun registerApplication(
        @Field("client_name") client_name: String,
        @Field("redirect_uris") redirect_uris: String,
        @Field("scopes") scopes: String? = null,
        @Field("website") website: String? = null
    ): Call<Application>

    @GET("/api/v1/timelines/public")
    fun timelinePublic(
        @Query("local") local: Boolean? = null,
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("min_id") min_id: String? = null,
        @Query("limit") limit: Int? = null
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

