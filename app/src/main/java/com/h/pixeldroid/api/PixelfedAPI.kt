package com.h.pixeldroid.api

import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Application
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.objects.Token
import io.reactivex.Single
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*


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

    @POST("api/v1/statuses/{id}/favourite")
    fun likePost(
        //The authorization header needs to be of the form "Bearer <token>"
        @Header("Authorization") authorization: String,
        @Path("id") statusId: String
    ) : Call<Status>

    @POST("/api/v1/statuses/{id}/unfavourite")
    fun unlikePost(
        //The authorization header needs to be of the form "Bearer <token>"
        @Header("Authorization") authorization: String,
        @Path("id") statusId: String
    ) : Call<Status>

    @POST("/api/v1/statuses/{id}/favourited_by")
    fun postLikedBy(
        @Path("id") statusId: String
    ) : Call<List<Account>>

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
}

