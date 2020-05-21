package com.h.pixeldroid.testUtility

import okhttp3.HttpUrl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

class MockServer {

    companion object{
        private val server = MockWebServer()
        private const val headerName = "Content-Type"
        private const val headerValue = "application/json; charset=utf-8"
    }

    fun start() {
        try {
            server.dispatcher = getDispatcher()
            server.start()
        } catch (e: IllegalArgumentException) {

        }
    }

    private fun getDispatcher(): Dispatcher {
        return object : Dispatcher() {
            @Throws(InterruptedException::class)
            override fun dispatch(request: RecordedRequest): MockResponse {
                when (request.path) {
                    "/api/v1/accounts/verify_credentials" -> return MockResponse()
                        .addHeader(headerName, headerValue)
                        .setResponseCode(200).setBody(JsonValues.accountJson)
                    "/api/v1/instance" -> return MockResponse()
                        .addHeader(headerName, headerValue)
                        .setResponseCode(200).setBody(JsonValues.instanceJson.replace("REPLACEWITHDOMAIN", getUrl().toString()))
                    "/api/v1/media" -> return MockResponse()
                        .addHeader(headerName, headerValue)
                        .setResponseCode(200).setBody(JsonValues.mediaUploadResponseJson)
                    "/api/v1/timelines/home" -> return MockResponse()
                        .addHeader(headerName, headerValue)
                        .setResponseCode(200).setBody(JsonValues.feedJson)
                    "/oauth/token" -> return MockResponse()
                        .addHeader(headerName, headerValue)
                        .setResponseCode(200).setBody(JsonValues.tokenJson)
                }
                when {
                    request.path?.startsWith("/api/v1/apps") == true -> {
                        return MockResponse()
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .setResponseCode(200).setBody(JsonValues.applicationJson)
                    }
                    request.path?.startsWith("/api/v1/notifications") == true -> {
                        return MockResponse()
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .setResponseCode(200).setBody(JsonValues.notificationsJson)
                    }
                    request.path?.startsWith("/api/v1/timelines/home") == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.feedJson)
                    }
                    request.path?.startsWith("/api/v1/timelines/public") == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.feedJson)
                    }
                    request.path?.startsWith("/api/v1/accounts/0/statuses") == true -> {
                        return MockResponse().setHttp2ErrorCode(401)
                    }
                    request.path?.matches("/api/v1/accounts/[0-9]*/statuses".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.accountStatusesJson)
                    }
                    request.path?.startsWith("/api/v1/statuses/0/context") == true -> {
                        return MockResponse().setHttp2ErrorCode(401)
                    }
                    request.path?.matches("/api/v1/statuses/[0-9]*/context".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.commentStatusesJson)
                    }
                    request.path?.startsWith("/api/v1/statuses/0/favourite") == true -> {
                        return MockResponse().setHttp2ErrorCode(401)
                    }
                    request.path?.matches("/api/v1/statuses/[0-9]*/favourite".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.likedJson)
                    }
                    request.path?.startsWith("/api/v1/statuses/0/unfavourite") == true -> {
                        return MockResponse().setHttp2ErrorCode(401)
                    }
                    request.path?.matches("/api/v1/statuses/[0-9]*/unfavourite".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.unlikeJson)
                    }
                    request.path?.startsWith("/api/v1/statuses") == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.unlikeJson)
                    }
                    request.path?.startsWith("/api/v1/accounts/0") == true -> {
                        return MockResponse().setHttp2ErrorCode(401)
                    }
                    request.path?.matches("/api/v1/accounts/[0-9]*".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.accountJson)
                    }
                    request.path?.startsWith("/api/v1/statuses/0/reblog") == true -> {
                        return MockResponse().setHttp2ErrorCode(401)
                    }
                    request.path?.matches("/api/v1/statuses/[0-9]*/reblog".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.reblogJson)
                    }
                    request.path?.startsWith("/api/v1/statuses/0/unreblog") == true -> {
                        return MockResponse().setHttp2ErrorCode(401)
                    }
                    request.path?.matches("/api/v1/statuses/[0-9]*/unreblog".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.unlikeJson)
                    }
                    request.path?.matches("/api/v1/accounts/[0-9]*/follow".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.followRelationshipJson)
                    }
                    request.path?.matches("/api/v1/accounts/[0-9]*/unfollow".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.unfollowRelationshipJson)
                    }
                    request.path?.startsWith("/api/v1/accounts/relationships") == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.relationshipJson)
                    }
                    request.path?.matches("/api/v1/accounts/[0-9]*/followers\\?limit=[0-9]*".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.followersJson)
                    }
                    request.path?.matches("/api/v1/accounts/[0-9]*/followers\\?since_id=[0-9]*&limit=[0-9]*".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.followersAfterJson)
                    }
                    request.path?.matches("/api/v1/accounts/[0-9]*/following\\?limit=[0-9]*".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.followersJson)
                    }
                    request.path?.matches("/api/v1/accounts/[0-9]*/following\\?since_id=[0-9]*&limit=[0-9]*".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.followersAfterJson)
                    }
                    request.path?.matches("/api/v2/search\\?type=hashtags&q=caturday&limit=[0-9]*&offset=[0-9]*".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.searchEmpty)
                    }
                    request.path?.startsWith("/api/v2/search?type=hashtags&q=caturday")!!-> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.searchCaturdayHashtags)
                    }
                    request.path?.startsWith("/api/v2/search?type=statuses&q=caturday")!! -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.searchCaturday)
                    }
                    request.path?.startsWith("/api/v2/search?type=accounts&q=dansup")!! -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.searchDansupAccounts)
                    }
                    request.path?.matches("""/api/v2/search\?(max_id=[0-9]*&)?type=(accounts|statuses)&q=dansup(&limit=[0-9]*)?""".toRegex())!! -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.searchEmpty)
                    }
                    request.path?.startsWith("/api/v2/discover/posts")!! -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.discover)
                    }
                    request.path?.startsWith("/api/v1/bookmarks")!! -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.bookmarkedJson)
                    }
                    request.path?.matches("/api/v1/statuses/[0-9]*/bookmark".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.bookmarkJson)
                    }
                    request.path?.matches("/api/v1/statuses/[0-9]*/unbookmark".toRegex()) == true -> {
                        return MockResponse().addHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        ).setResponseCode(200).setBody(JsonValues.unbookmarkJson)
                    }
                    else -> return MockResponse().setResponseCode(404)
                }
            }
        }
    }

    fun getUrl(): HttpUrl {
        return server.url("")
    }

}