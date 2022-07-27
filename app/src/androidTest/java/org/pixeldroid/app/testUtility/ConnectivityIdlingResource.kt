package org.pixeldroid.app.testUtility

import android.content.Context
import androidx.test.espresso.IdlingResource
import kotlin.jvm.Volatile
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import org.pixeldroid.app.testUtility.ConnectivityIdlingResource

/**
 * Created by Kush Saini
 * Later converted and adapted to Kotlin for PixelDroid
 * Description:
 * This [IdlingResource] halts Espresso Test depending on mode which is passed to the constructor
 */
class ConnectivityIdlingResource
/**
 *
 * @param resourceName              name of the resource (used for logging and idempotency of registration
 * @param context                   context
 * @param mode                      if mode is WAIT_FOR_CONNECTION i.e. value is 1 then the [IdlingResource]
 * halts test until internet is available and if mode is WAIT_FOR_DISCONNECTION
 * i.e. value is 0 then [IdlingResource] waits for internet to disconnect
 */(private val resourceName: String, private val context: Context, private val mode: Int) :
    IdlingResource {
    @Volatile
    private var resourceCallback: IdlingResource.ResourceCallback? = null
    override fun getName(): String {
        return resourceName
    }

    override fun isIdleNow(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        var isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting
        if (mode == WAIT_FOR_DISCONNECTION) isConnected = !isConnected
        if (isConnected) {
            Log.d(TAG, "Connected now!")
            resourceCallback!!.onTransitionToIdle()
        } else {
            Log.d(TAG, "Not connected!")
        }
        return isConnected
    }

    override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback) {
        this.resourceCallback = resourceCallback
    }

    companion object {
        var WAIT_FOR_CONNECTION = 1
        var WAIT_FOR_DISCONNECTION = 0
        private const val TAG = "ConnectIdlingResource"
    }
}