package com.h.pixeldroid.utils

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Account.Companion.getAccountFromId
import com.h.pixeldroid.objects.Mention
import com.h.pixeldroid.utils.customSpans.ClickableSpanNoUnderline
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale


class HtmlUtils {
    companion object {

        private fun fromHtml(html: String): Spanned {
            val result: Spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml(html)
            }
            return result.trim() as Spanned
        }

        private fun getDomain(urlString: String?): String {
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
            credential: String
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
                        if (mention.username.toLowerCase(Locale.ROOT)
                            == accountUsername.toLowerCase(Locale.ROOT)
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
                                getAccountFromId(accountId, api, context, credential)
                            }
                        }
                    }
                }

                builder.removeSpan(span);
                builder.setSpan(customSpan, start, end, flags);

                // Add zero-width space after links in end of line to fix its too large hitbox.
                if (end >= builder.length || builder.subSequence(end, end + 1).toString() == "\n") {
                    builder.insert(end, "\u200B")
                }
            }

            return builder
        }
    }
}