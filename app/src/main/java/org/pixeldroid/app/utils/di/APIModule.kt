package org.pixeldroid.app.utils.di

import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Token
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.runBlocking
import okhttp3.*
import org.pixeldroid.app.utils.api.PixelfedAPI.Companion.apiForUser
import javax.inject.Singleton

@Module
class APIModule{

    @Provides
    @Singleton
    fun providesAPIHolder(db: AppDatabase): PixelfedAPIHolder {
        return PixelfedAPIHolder(db)
    }
}

class TokenAuthenticator(val user: UserDatabaseEntity, val db: AppDatabase, val apiHolder: PixelfedAPIHolder) : Authenticator {

    private val pixelfedAPI = PixelfedAPI.createFromUrl(user.instance_uri)

    // Returns the number of tries for this response by walking through the priorResponses
    private fun Response.responseCount(): Int {
        var result = 1
        var response: Response? = priorResponse

        while (response != null) {
            result++
            response = response.priorResponse
        }
        return result
    }


    override fun authenticate(route: Route?, response: Response): Request? {

        if (response.responseCount() > 3) {
            return null // Give up, we've already failed to authenticate a couple times
        }
        // Refresh the access_token using a synchronous api request
        val newAccessToken: Token = try {
            runBlocking {
                pixelfedAPI.obtainToken(
                    scope = "",
                    grant_type = "refresh_token",
                    refresh_token = user.refreshToken,
                    client_id = user.clientId,
                    client_secret = user.clientSecret
                )
            }
        }catch (e: Exception){
            return null
        }

        // Save the new access token and refresh token
        if (newAccessToken.access_token != null && newAccessToken.refresh_token != null) {
            db.userDao().updateAccessToken(
                    newAccessToken.access_token,
                    newAccessToken.refresh_token,
                    user.user_id, user.instance_uri
            )
            apiHolder.setToCurrentUser()
        }

        // Add new header to rejected request and retry it
        return response.request.newBuilder()
                .header("Authorization", "Bearer ${newAccessToken.access_token.orEmpty()}")
                .build()
    }
}

class PixelfedAPIHolder(private val db: AppDatabase){

    var api: PixelfedAPI? =
        db.userDao().getActiveUser()?.let {
            setToCurrentUser(it)
        }

    fun setToCurrentUser(
        user: UserDatabaseEntity = db.userDao().getActiveUser()!!
    ): PixelfedAPI {
        val newAPI = apiForUser(user, db, this)
        api = newAPI
        return newAPI
    }
}