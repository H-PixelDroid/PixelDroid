package org.pixeldroid.app.utils.api

import com.google.gson.*
import io.reactivex.rxjava3.core.Observable
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import org.pixeldroid.app.utils.api.objects.*
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import org.pixeldroid.app.searchDiscover.TrendingActivity
import org.pixeldroid.app.utils.api.objects.Collection
import org.pixeldroid.app.utils.api.objects.Tag
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import org.pixeldroid.app.utils.di.TokenAuthenticator
import org.pixeldroid.app.utils.typeAdapterInstantDeserializer
import org.pixeldroid.app.utils.typeAdapterInstantSerializer
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.http.Field
import java.time.Instant
import java.util.concurrent.TimeUnit


/*
    Implements the Pixelfed API
    https://docs.pixelfed.org/technical-documentation/api-v1
    However, since this is mostly based on the Mastodon API, the documentation there
    will be more useful: https://docs.joinmastodon.org/
 */

interface PixelfedAPI {


    companion object {
        val headerInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", "PixelDroid")
            chain.proceed(requestBuilder.build())
        }

        fun createFromUrl(baseUrl: String): PixelfedAPI {
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gSonInstance))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .client(
                    OkHttpClient().newBuilder().addNetworkInterceptor(headerInterceptor)
                        // Only do secure-ish TLS connections (no HTTP or very old SSL/TLS)
                        .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
                        .readTimeout(20, TimeUnit.SECONDS)
                        .build()
                )
                .build().create(PixelfedAPI::class.java)
        }

        private val gSonInstance: Gson = GsonBuilder()
            .registerTypeAdapter(Instant::class.java, typeAdapterInstantDeserializer)
            .registerTypeAdapter(Instant::class.java, typeAdapterInstantSerializer)
            .create()

        fun apiForUser(
            user: UserDatabaseEntity,
            db: AppDatabase,
            pixelfedAPIHolder: PixelfedAPIHolder
        ): PixelfedAPI =
            Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gSonInstance))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .baseUrl(user.instance_uri)
                .client(
                    OkHttpClient().newBuilder().addNetworkInterceptor(headerInterceptor)
                            // Only do secure-ish TLS connections (no HTTP or very old SSL/TLS)
                        .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
                        .readTimeout(20, TimeUnit.SECONDS)
                        .authenticator(TokenAuthenticator(user, db, pixelfedAPIHolder))
                        .addInterceptor {
                            it.request().newBuilder().run {
                                header("Accept", "application/json")
                                header("Authorization", "Bearer ${user.accessToken}")
                                it.proceed(build())
                            }
                        }.build()
                )
                .build().create(PixelfedAPI::class.java)
    }


    @FormUrlEncoded
    @POST("/api/v1/apps")
    suspend fun registerApplication(
        @Field("client_name") client_name: String,
        @Field("redirect_uris") redirect_uris: String,
        @Field("scopes") scopes: String? = null,
        @Field("website") website: String? = null
    ): Application


    @FormUrlEncoded
    @POST("/oauth/token")
    suspend fun obtainToken(
        @Field("client_id") client_id: String,
        @Field("client_secret") client_secret: String,
        @Field("redirect_uri") redirect_uri: String? = null,
        @Field("scope") scope: String? = "read",
        @Field("code") code: String? = null,
        @Field("grant_type") grant_type: String? = null,
        @Field("refresh_token") refresh_token: String? = null
    ): Token

    // get instance configuration
    @GET("/api/v1/instance")
    suspend fun instance() : Instance

    /**
     * Instance info from the Nodeinfo .well_known (https://nodeinfo.diaspora.software/protocol.html) endpoint
     */
    @GET("/.well-known/nodeinfo")
    suspend fun wellKnownNodeInfo() : NodeInfoJRD

    /**
     * Instance info from [NodeInfo] (https://nodeinfo.diaspora.software/schema.html) endpoint
     */
    @GET
    suspend fun nodeInfoSchema(
            @Url nodeInfo_schema_url: String
    ) : NodeInfo

    @FormUrlEncoded
    @POST("/api/v1/accounts/{id}/follow")
    suspend fun follow(
        @Path("id") statusId: String,
        @Field("reblogs") reblogs : Boolean = true
    ) : Relationship

    @POST("/api/v1/accounts/{id}/unfollow")
    suspend fun unfollow(
        @Path("id") statusId: String,
    ) : Relationship

    @POST("api/v1/statuses/{id}/favourite")
    suspend fun likePost(
        @Path("id") statusId: String

    ) : Status

    @POST("/api/v1/statuses/{id}/unfavourite")
    suspend fun unlikePost(
        @Path("id") statusId: String
    ) : Status

    //Used in our case to post a comment or a status
    @FormUrlEncoded
    @POST("/api/v1/statuses")
    suspend fun postStatus(
        @Field("status") statusText: String,
        @Field("in_reply_to_id") in_reply_to_id: String? = null,
        @Field("media_ids[]") media_ids: List<String> = emptyList(),
        @Field("poll[options][]") poll_options: List<String>? = null,
        @Field("poll[expires_in]") poll_expires: List<String>? = null,
        @Field("poll[multiple]") poll_multiple: List<String>? = null,
        @Field("poll[hide_totals]") poll_hideTotals: List<String>? = null,
        //FIXME this should be able to take a boolean or at least "true"/"false" but only "0"/"1" works
        @Field("sensitive") sensitive: Int? = null,
        @Field("spoiler_text") spoiler_text: String? = null,
        @Field("visibility") visibility: String = "public",
        @Field("scheduled_at") scheduled_at: String? = null,
        @Field("language") language: String? = null
    ) : Status

    @DELETE("/api/v1/statuses/{id}")
    suspend fun deleteStatus(
            @Path("id") statusId: String
    )

    @FormUrlEncoded
    @POST("/api/v1/statuses/{id}/reblog")
    suspend fun reblogStatus(
        @Path("id") statusId: String,
        @Field("visibility") visibility: String? = null
    ) : Status

    @POST("/api/v1/statuses/{id}/unreblog")
    suspend fun undoReblogStatus(
        @Path("id") statusId: String,
    ) : Status

    @GET("/api/v1/bookmarks")
    suspend fun bookmarks(
        @Query("limit") limit: Number? = null,
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("min_id") min_id: String? = null
    ) : List<Status>

    @POST("/api/v1/statuses/{id}/bookmark")
    suspend fun bookmarkStatus(
        @Path("id") statusId: String
    ) : Status

    @POST("/api/v1/statuses/{id}/unbookmark")
    suspend fun undoBookmarkStatus(
        @Path("id") statusId: String
    ) : Status

    @GET("/api/v1.1/collections/accounts/{id}")
    suspend fun accountCollections(
        @Path("id") account_id: String? = null
    ): List<Collection>

    @GET("/api/v1.1/collections/items/{id}")
    suspend fun collectionItems(
        @Path("id") id: String,
        @Query("page") page: String? = null
    ): List<Status>

    @DELETE("/api/v1.1/collections/delete/{id}")
    suspend fun deleteCollection(
        @Path("id") id: String,
    )

    @POST("/api/v1.1/collections/add")
    suspend fun addToCollection(
        @Query("collection_id") collection_id: String,
        @Query("post_id") post_id: String,
    ): Status

    @POST("/api/v1.1/collections/remove")
    suspend fun removeFromCollection(
        @Query("collection_id") collection_id: String,
        @Query("post_id") post_id: String,
    )

    @GET("/api/pixelfed/v1/stories/self-carousel")
    suspend fun carousel(): StoryCarousel

    @POST("/api/v1.1/stories/seen")
    suspend fun storySeen(
        @Query("id") id: String
    )

    @POST("/api/v1.1/stories/comment")
    suspend fun storyComment(
        @Query("sid") sid: String,
        @Query("caption") caption: String
    )

    @Multipart
    @POST("/api/v1.1/stories/add")
    fun storyUpload(
        @Part file: MultipartBody.Part,
        // The API takes this value but then overwrites it in /api/v1.1/stories/publish, so ignore this
        @Part duration: MultipartBody.Part? = null,
    ): Observable<Attachment>

    @POST("/api/v1.1/stories/publish")
    suspend fun storyPublish(
        @Query("media_id") media_id: String,
        //From 0 to 30, duration in seconds of the story
        @Query("duration") duration: Int = 10,
        //FIXME this should be able to take a boolean or at least "true"/"false" but only "0"/"1" works. Same issue as sensitive boolean in postStatus
        @Query("can_reply") can_reply: String,
        @Query("can_react") can_react: String,
    )

    @POST("/api/v1.1/stories/self-expire/{id}")
    suspend fun deleteCarousel(
        @Path("id") storyId: String
    )

    //Used in our case to retrieve comments for a given status
    @GET("/api/v1/statuses/{id}/context")
    suspend fun statusComments(
        @Path("id") statusId: String,
    ) : Context

    @GET("/api/v1/timelines/public")
    suspend fun timelinePublic(
        @Query("local") local: Boolean? = null,
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("min_id") min_id: String? = null,
        @Query("limit") limit: String? = null
    ): List<Status>

    @GET("/api/v1/timelines/home")
    suspend fun timelineHome(
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("min_id") min_id: String? = null,
        @Query("limit") limit: String? = null,
        @Query("local") local: Boolean? = null
    ): List<Status>

    @GET("/api/v1/timelines/tag/{hashtag}")
    suspend fun hashtag(
        @Path("hashtag") hashtag: String? = null,
        @Query("local") local: Boolean? = null,
        @Query("only_media") only_media: Boolean? = null,
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("min_id") min_id: String? = null,
        @Query("limit") limit: Int? = null,
    ): List<Status>

    @GET("/api/v2/search")
    suspend fun search(
        @Query("account_id") account_id: String? = null,
        @Query("max_id") max_id: String? = null,
        @Query("min_id") min_id: String? = null,
        @Query("type") type: Results.SearchType? = null,
        @Query("exclude_unreviewed") exclude_unreviewed: Boolean? = null,
        @Query("q") q: String,
        @Query("resolve") resolve: Boolean? = null,
        @Query("limit") limit: String? = null,
        @Query("offset") offset: String? = null,
        @Query("following") following: Boolean? = null
    ): Results

    @GET("/api/v1/notifications")
    suspend fun notifications(
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("min_id") min_id: String? = null,
        @Query("limit") limit: String? = null,
        @Query("exclude_types") exclude_types: List<String>? = null,
        @Query("account_id") account_id: Boolean? = null
    ): List<Notification>

    @GET("/api/v1/accounts/verify_credentials")
    suspend fun verifyCredentials(
        //The authorization header needs to be of the form "Bearer <token>"
        @Header("Authorization") authorization: String? = null
    ): Account

    //@Multipart
    @PATCH("/api/v1/accounts/update_credentials")
    suspend fun updateCredentials(
        @Query(value = "display_name") displayName: String?,
        @Query(value = "note") note: String?,
        @Query(value = "locked") locked: Boolean?,
      //  @Part avatar: MultipartBody.Part?,
    ): Account

    @Multipart
    @POST("/api/v1/accounts/update_credentials")
    fun updateProfilePicture(
        @Part avatar: MultipartBody.Part?
    ): Observable<Account>

    @GET("/api/v1/accounts/{id}/statuses")
    suspend fun accountPosts(
            @Path("id") account_id: String,
            @Query("min_id") min_id: String? = null,
            @Query("max_id") max_id: String?,
            @Query("limit") limit: Int
    ) : List<Status>

    @GET("/api/v1/accounts/relationships")
    suspend fun checkRelationships(
        @Query("id[]") account_ids : List<String>
    ) : List<Relationship>

    @GET("/api/v1/accounts/{id}/followers")
    suspend fun followers(
        @Path("id") account_id: String,
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("limit") limit: Number? = null,
        @Query("page") page: String? = null
    ) : Response<List<Account>>

    @GET("/api/v1/accounts/{id}/following")
    suspend fun following(
        @Path("id") account_id: String,
        @Query("max_id") max_id: String? = null,
        @Query("since_id") since_id: String? = null,
        @Query("limit") limit: Number? = 40,
        @Query("page") page: String? = null
    ) : Response<List<Account>>

    @GET("/api/v1/accounts/{id}")
    suspend fun getAccount(
        @Path("id") accountId : String
    ): Account

    @GET("/api/v1/statuses/{id}")
    suspend fun getStatus(
        @Path("id") accountId : String
    ): Status

    @Multipart
    @POST("/api/v1/media")
    fun mediaUpload(
        @Part description: MultipartBody.Part? = null,
        @Part file: MultipartBody.Part
    ): Observable<Attachment>

    // get discover
    @GET("/api/v1/discover/posts")
    suspend fun discover() : DiscoverPosts

    @GET("/api/v1.1/discover/accounts/popular")
    suspend fun popularAccounts() : List<Account>

    @GET("/api/v1.1/discover/posts/trending")
    suspend fun trendingPosts(
        @Query("range") range: TrendingActivity.Companion.Range
    ) : List<Status>

    @GET("/api/v1.1/discover/posts/hashtags")
    suspend fun trendingHashtags() : List<Tag>

    @FormUrlEncoded
    @POST("/api/v1/reports")
    @JvmSuppressWildcards
    suspend fun report(
        @Field("account_id") account_id: String,
        @Field("status_ids") status_ids: List<Status>,
        @Field("comment") comment: String,
        @Field("forward") forward: Boolean = true
    ) : Report

}
