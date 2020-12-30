package com.h.pixeldroid.utils.di

import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.db.AppDatabase
import com.h.pixeldroid.utils.db.addUser
import com.h.pixeldroid.utils.db.entities.UserDatabaseEntity
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.runBlocking
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
        return PixelfedAPIHolder(db)
    }
}

class TokenAuthenticator(val user: UserDatabaseEntity, val db: AppDatabase) : Authenticator {

    private val pixelfedAPI = PixelfedAPI.createFromUrl(user.instance_uri)

    override fun authenticate(route: Route?, response: Response): Request? {

        if (response.request.header("Authorization") != null) {
            return null // Give up, we've already failed to authenticate.
        }
        // Refresh the access_token using a synchronous api request
        val newAccessToken: String? = try {
            runBlocking {
                pixelfedAPI.obtainToken(
                    scope = "",
                    grant_type = "refresh_token",
                    refresh_token = user.refreshToken,
                    client_id = user.clientId,
                    client_secret = user.clientSecret
                ).access_token
            }
        }catch (e: Exception){
            null
        }

        if (newAccessToken != null) {
            db.userDao().updateAccessToken(newAccessToken, user.user_id, user.instance_uri)
        }

        // Add new header to rejected request and retry it
        return response.request.newBuilder()
                .header("Authorization", "Bearer ${newAccessToken.orEmpty()}")
                .build()
    }
}

class PixelfedAPIHolder(db: AppDatabase?){
    private val intermediate: Retrofit.Builder = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    var api: PixelfedAPI? =
        db?.userDao()?.getActiveUser()?.let {
            setDomainToCurrentUser(db, it)
        }

    fun setDomainToCurrentUser(
        db: AppDatabase,
        user: UserDatabaseEntity = db.userDao().getActiveUser()!!
    ): PixelfedAPI {
        val newAPI = intermediate
            .baseUrl(user.instance_uri)
            .client(OkHttpClient().newBuilder().authenticator(TokenAuthenticator(user, db)).build())
            .build().create(PixelfedAPI::class.java)
        api = newAPI
        return newAPI
    }
}