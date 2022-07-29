package org.pixeldroid.app.utils

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import org.pixeldroid.app.utils.api.PixelfedAPI
import java.io.InputStream

@GlideModule
class PixelDroidGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val client: OkHttpClient = OkHttpClient().newBuilder()
            // Only do secure-ish TLS connections (no HTTP or very old SSL/TLS)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .addNetworkInterceptor(PixelfedAPI.headerInterceptor)
            .build()
        val factory = OkHttpUrlLoader.Factory(client)
        glide.registry.replace(GlideUrl::class.java, InputStream::class.java, factory)
    }
}
