package org.pixeldroid.app.utils.api.objects

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import org.pixeldroid.app.profile.ProfileActivity
import org.pixeldroid.app.utils.api.PixelfedAPI
import retrofit2.HttpException
import java.io.IOException
import java.io.Serializable
import java.time.Instant

/*
Represents a user and their associated profile.
https://docs.joinmastodon.org/entities/account/
 */

data class Account(
    //Base attributes
    override val id: String?,
    val username: String?,
    val acct: String? = "",
    val url: String? = "", //HTTPS URL
    //Display attributes
    val display_name: String? = "",
    val note: String? = "", //HTML
    private val avatar: String? = "", //URL
    private val avatar_static: String? = "", //URL
    val header: String? = "", //URL
    val header_static: String? = "", //URL
    val locked: Boolean? = false,
    val emojis: List<Emoji>? = null,
    val discoverable: Boolean? = true,
    //Statistical attributes
    val created_at: Instant? = null, //ISO 8601 Datetime
    val statuses_count: Int? = 0,
    val followers_count: Int? = 0,
    val following_count: Int? = 0,
    //Optional attributes
    val moved: Account? = null,
    val fields: List<Field>? = emptyList(),
    val bot: Boolean? =  false,
    val source: Source? = null,
    val suspended: Boolean? = null,
    val mute_expires_at: Instant? = null, //ISO 8601 Datetime
) : Serializable, FeedContent {
    companion object {
        const val ACCOUNT_TAG = "AccountTag"
        const val ACCOUNT_ID_TAG = "AccountIdTag"
        const val FOLLOWERS_TAG = "FollowingTag"


        /**
         * @brief Opens an activity of the profile with the given id
         */
        suspend fun openAccountFromId(id: String, api : PixelfedAPI, context: Context) {
                val account = try {
                    api.getAccount(id)
                } catch (exception: IOException) {
                    Log.e("GET ACCOUNT ERROR", exception.toString())
                    return
                } catch (exception: HttpException) {
                    Log.e("ERROR CODE", exception.code().toString())
                    return
                }
                //Open the account page in a separate activity
                account.openProfile(context)

        }
    }

    fun getDisplayName() : String = when {
        username.isNullOrBlank() && display_name.isNullOrBlank() -> ""
        display_name.isNullOrBlank() -> "@$username"
        else -> display_name
    }

    fun anyAvatar(): String? = avatar_static ?: avatar
    /**
     * @brief Open profile activity with given account
     */
    fun openProfile(context: Context) {
        val intent = Intent(context, ProfileActivity::class.java)
        intent.putExtra(ACCOUNT_TAG, this as Serializable)
        startActivity(context, intent, null)
    }
}
