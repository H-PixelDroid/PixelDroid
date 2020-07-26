package com.h.pixeldroid.di

import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.db.AppDatabase
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
class APIModule{

    @Provides
    @Singleton
    fun providesAPIHolder(db: AppDatabase): PixelfedAPIHolder {
        return PixelfedAPIHolder(db.userDao().getActiveUser()?.instance_uri)
    }

}


class PixelfedAPIHolder(domain: String?){
    private val intermediate: Retrofit.Builder = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    var api: PixelfedAPI? = if (domain != null) setDomain(domain) else null

    fun setDomainToCurrentUser(db: AppDatabase): PixelfedAPI {
        return setDomain(db.userDao().getActiveUser()!!.instance_uri)
    }

    fun setDomain(domain: String): PixelfedAPI {
        val newAPI = intermediate
            .baseUrl(domain)
            .build().create(PixelfedAPI::class.java)
        api = newAPI
        return newAPI
    }
}