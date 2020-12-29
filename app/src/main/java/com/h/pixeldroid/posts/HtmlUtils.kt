package com.h.pixeldroid.posts

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.toSpanned
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import com.h.pixeldroid.R
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.api.objects.Account.Companion.openAccountFromId
import com.h.pixeldroid.utils.api.objects.Mention
import kotlinx.coroutines.coroutineScope
import java.net.URI
import java.net.URISyntaxException
import java.text.ParseException
import java.util.*

fun fromHtml(html: String): Spanned {
    val result: Spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(html)
    }
    return result.trim().toSpanned()
}

fun getDomain(urlString: String?): String {
    val uri: URI
    try {
        uri = URI(urlString!!)
    } catch (e: URISyntaxException) {
        return ""
    }
    val host: String = uri.host
    return if (host.startsWith("www.")) {
        host.substring(4)
    } else {
        host
    }
}

fun parseHTMLText(
        text : String,
        mentions: List<Mention>?,
        api : PixelfedAPI,
        context: Context,
        credential: String,
        lifecycleScope: LifecycleCoroutineScope
) : Spanned {
    //Convert text to spannable
    val content = fromHtml(text)

    //Retrive all links that should be made clickable
    val builder = SpannableStringBuilder(content)
    val urlSpans = content.getSpans(0, content.length, URLSpan::class.java)

    for(span in urlSpans) {
        val start = builder.getSpanStart(span)
        val end = builder.getSpanEnd(span)
        val flags = builder.getSpanFlags(span)
        val text = builder.subSequence(start, end)
        var customSpan: ClickableSpan? = null

        //Handle hashtags
        if (text[0] == '#') {
            val tag = text.subSequence(1, text.length).toString()
            customSpan = object : ClickableSpanNoUnderline() {
                override fun onClick(widget: View) {
                    Toast.makeText(context, tag, Toast.LENGTH_SHORT).show()
                }

            }
        }

        //Handle mentions
        if(text[0] == '@' && !mentions.isNullOrEmpty()) {
            val accountUsername = text.subSequence(1, text.length).toString()
            var id: String? = null

            //Go through all mentions stored in the status
            for (mention in mentions) {
                if (mention.username.equals(accountUsername, ignoreCase = true)
                ) {
                    id = mention.id

                    //Mentions can be of users in other domains
                    if (mention.url.contains(getDomain(span.url))) {
                        break
                    }
                }
            }

            //Check that we found a user for the given mention
            if (id != null) {
                val accountId: String = id
                customSpan = object : ClickableSpanNoUnderline() {
                    override fun onClick(widget: View) {
                        Log.e("MENTION", "CLICKED")
                        //Retrieve the account for the given profile
                        lifecycleScope.launchWhenCreated {
                            openAccountFromId(accountId, api, context, credential)
                        }
                    }
                }
            }
        }

        builder.removeSpan(span)
        builder.setSpan(customSpan, start, end, flags)

        // Add zero-width space after links in end of line to fix its too large hitbox.
        if (end >= builder.length || builder.subSequence(end, end + 1).toString() == "\n") {
            builder.insert(end, "\u200B")
        }
    }

    return builder
}


fun setTextViewFromISO8601(date: Date, textView: TextView, absoluteTime: Boolean, context: Context) {
    val now = Date().time

    try {
        val then = date.time
        val formattedDate = android.text.format.DateUtils
                .getRelativeTimeSpanString(then, now,
                        android.text.format.DateUtils.SECOND_IN_MILLIS,
                        android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE)

        textView.text = if(absoluteTime) context.getString(R.string.posted_on).format(date)
        else "$formattedDate"

    } catch (e: ParseException) {
        e.printStackTrace()
    }
}