package com.h.pixeldroid.di

import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.entities.UserDatabaseEntity
import dagger.Module
import dagger.Provides
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception
import javax.inject.Singleton

@Module
class APIModule{

    @Provides
    @Singleton
    fun providesAPIHolder(db: AppDatabase): PixelfedAPIHolder {
        return PixelfedAPIHolder(db.userDao().getActiveUser())
    }
}

class TokenAuthenticator(val user: UserDatabaseEntity) : Authenticator {

    val pixelfedAPI = PixelfedAPI.createFromUrl(user.instance_uri)

    override fun authenticate(route: Route?, response: Response): Request? {

        if (response.request.header("Authorization") != null) {
            return null // Give up, we've already failed to authenticate.
        }
        // Refresh the access_token using a synchronous api request
        val newAccessToken: String =  try {
            pixelfedAPI.obtainToken(
                    scope = "", grant_type = "refresh_token",
                    refresh_token = user.refreshToken, client_id = user.clientId, client_secret = user.clientSecret
            ).blockingGet().access_token
        }catch (e: Exception){
            null
        }.orEmpty()

        // Add new header to rejected request and retry it
        return response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
    }
}

class PixelfedAPIHolder(user: UserDatabaseEntity?){
    private val intermediate: Retrofit.Builder = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    var api: PixelfedAPI? = if (user != null) setDomain(user) else null

    fun setDomainToCurrentUser(db: AppDatabase): PixelfedAPI {
        return setDomain(db.userDao().getActiveUser()!!)
    }

    fun setDomain(user: UserDatabaseEntity): PixelfedAPI {
        val newAPI = intermediate
                .baseUrl(user.instance_uri)
                .client(OkHttpClient().newBuilder().authenticator(TokenAuthenticator(user)).build())
                .build().create(PixelfedAPI::class.java)
        api = newAPI
        return newAPI
    }
}