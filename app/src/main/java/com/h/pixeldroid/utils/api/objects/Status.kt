package com.h.pixeldroid.utils.api.objects

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.Environment
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.net.toUri
import com.google.android.material.snackbar.Snackbar
import com.h.pixeldroid.R
import com.h.pixeldroid.databinding.PostFragmentBinding
import com.h.pixeldroid.posts.getDomain
import java.io.File
import java.io.Serializable
import java.util.*

/**
    Represents a status posted by an account.
    https://docs.joinmastodon.org/entities/status/
 */

open class Status(
    //Base attributes
    override val id: String,
    val uri: String? = "",
    val created_at: Date? = Date(0), //ISO 8601 Datetime
    val account: Account?,
    val content: String? = "", //HTML
    val visibility: Visibility? = Visibility.public,
    val sensitive: Boolean? = false,
    val spoiler_text: String? = "",
    val media_attachments: List<Attachment>? = null,
    val application: Application? = null,
    //Rendering attributes
    val mentions: List<Mention>? = null,
    val tags: List<Tag>? = null,
    val emojis: List<Emoji>? = null,
    //Informational attributes
    val reblogs_count: Int? = 0,
    val favourites_count: Int? = 0,
    val replies_count: Int? = 0,
    //Nullable attributes
    val url: String? = null, //URL
    val in_reply_to_id: String? = null,
    val in_reply_to_account: String? = null,
    val reblog: Status? = null,
    val poll: Poll? = null,
    val card: Card? = null,
    val language: String? = null, //ISO 639 Part 1 two-letter language code
    val text: String? = null,
    //Authorized user attributes
    val favourited: Boolean? = false,
    val reblogged: Boolean? = false,
    val muted: Boolean? = false,
    val bookmarked: Boolean? = false,
    val pinned: Boolean? = false,
) : Serializable, FeedContent
{
    companion object {
        const val POST_TAG = "postTag"
        const val DOMAIN_TAG = "domainTag"
        const val DISCOVER_TAG = "discoverTag"
    }

    fun getPostUrl() : String? = media_attachments?.firstOrNull()?.url
    fun getProfilePicUrl() : String? = account?.avatar
    fun getPostPreviewURL() : String? = media_attachments?.firstOrNull()?.preview_url


    fun getNLikes(context: Context) : CharSequence {
        return context.resources.getQuantityString(
                R.plurals.likes,
                favourites_count ?: 0,
                favourites_count ?: 0
        )
    }

    fun getNShares(context: Context) : CharSequence {
        return context.resources.getQuantityString(
                R.plurals.shares,
                reblogs_count ?: 0,
                reblogs_count ?: 0
        )
    }

    fun getStatusDomain(domain: String) : String {
        val accountDomain = getDomain(account!!.url)
        return if(getDomain(domain) == accountDomain) ""
        else " from $accountDomain"

    }

    fun downloadImage(context: Context, url: String, view: View, share: Boolean = false) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadUri = Uri.parse(url)

        val title = url.substringAfterLast("/")
        val request = DownloadManager.Request(downloadUri).apply {
            setTitle(title)
            if(!share) {
                val directory = File(Environment.DIRECTORY_PICTURES)
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                setDestinationInExternalPublicDir(directory.toString(), title)
            }
        }
        val downloadId = downloadManager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(downloadId)

        Thread {

            var msg = ""
            var lastMsg = ""
            var downloading = true

            while (downloading) {
                val cursor: Cursor = downloadManager.query(query)
                cursor.moveToFirst()
                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    == DownloadManager.STATUS_SUCCESSFUL
                ) {
                    downloading = false
                }
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                if (!share) {
                    msg = when (status) {
                        DownloadManager.STATUS_FAILED ->
                            context.getString(R.string.image_download_failed)
                        DownloadManager.STATUS_RUNNING ->
                            context.getString(R.string.image_download_downloading)
                        DownloadManager.STATUS_SUCCESSFUL ->
                            context.getString(R.string.image_download_success)
                        else -> ""
                    }
                    if (msg != lastMsg && msg != "") {
                        Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                        lastMsg = msg
                    }
                } else if (status == DownloadManager.STATUS_SUCCESSFUL) {

                    val ext = url.substringAfterLast(".", "*")

                    val path = cursor.getString(
                        cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    )
                    val file = path.toUri()

                    val shareIntent: Intent = Intent.createChooser(Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, file)
                        data = file
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        type = "image/$ext"
                    }, null)

                    context.startActivity(shareIntent)
                }
                cursor.close()
            }
        }.start()
    }

    enum class Visibility: Serializable {
        public, unlisted, private, direct
    }
}