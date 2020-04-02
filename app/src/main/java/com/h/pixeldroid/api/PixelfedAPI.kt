package com.h.pixeldroid.api

import com.h.pixeldroid.objects.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.http.Field
import java.io.File


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

    @FormUrlEncoded
    @POST("/oauth/token")
    fun obtainToken(
        @Field("client_id") client_id: String,
        @Field("client_secret") client_secret: String,
        @Field("redirect_uri") redirect_uri: String,
        @Field("scope") scope: String? = "read",
        @Field("code") code: String? = null,
        @Field("grant_type") grant_type: String? = null
    ): Call<Token>

    @GET("/api/v1/timelines/public")
    fun timelinePublic(
        @Query("local") local: Boolean? = null,
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("min_id") min_id: String? = null,
        @Query("limit") limit: Int? = null
    ): Call<List<Status>>


    @GET("/api/v1/timelines/home")
    fun timelineHome(
        //The authorization header needs to be of the form "Bearer <token>"
        @Header("Authorization") authorization: String,
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("min_id") min_id: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("local") local: Boolean? = null
    ): Call<List<Status>>

    /*
    Note: as of 0.10.8, Pixelfed does not seem to respect the Mastodon API documentation,
    you *need* to pass one of the so-called "optional" arguments. See:
    https://github.com/pixelfed/pixelfed/blob/dev/app/Http/Controllers/Api/ApiV1Controller.php
    An example that works: specify min_id as 1 (not 0 though)
     */
    @GET("/api/v1/notifications")
    fun notifications(
        //The authorization header needs to be of the form "Bearer <token>"
        @Header("Authorization") authorization: String,
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("min_id") min_id: String? = null,
        @Query("exclude_types") limit: String? = null,
        @Query("account_id") exclude_types: Boolean? = null
    ): Call<List<Notification>>

    @GET("/api/v1/accounts/verify_credentials")
    fun verifyCredentials(
        //The authorization header needs to be of the form "Bearer <token>"
        @Header("Authorization") authorization: String
        ): Call<Account>

    companion object {
        fun create(baseUrl: String): PixelfedAPI {
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build().create(PixelfedAPI::class.java)
        }
    }

    @POST("/api/v1/media")
    fun mediaUpload(
        //The authorization header needs to be of the form "Bearer <token>"
        @Header("Authorization") authorization: String,
        @Field("file") file: File? = null
    ): Call<Attachment>
}

