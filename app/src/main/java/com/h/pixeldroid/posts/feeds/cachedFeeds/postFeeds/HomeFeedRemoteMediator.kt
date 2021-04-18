package com.h.pixeldroid.posts.feeds.cachedFeeds.postFeeds

import androidx.paging.*
import androidx.room.withTransaction
import com.h.pixeldroid.utils.db.AppDatabase
import com.h.pixeldroid.utils.di.PixelfedAPIHolder
import com.h.pixeldroid.utils.db.entities.HomeStatusDatabaseEntity
import retrofit2.HttpException
import java.io.IOException
import java.lang.NullPointerException
import javax.inject.Inject


/**
 * RemoteMediator for the home feed.
 *
 * A [RemoteMediator] defines a set of callbacks used to incrementally load data from a remote
 * source into a local source wrapped by a [PagingSource], e.g., loading data from network into
 * a local db cache.
 */
@OptIn(ExperimentalPagingApi::class)
class HomeFeedRemoteMediator @Inject constructor(
    private val apiHolder: PixelfedAPIHolder,
    private val db: AppDatabase,
) : RemoteMediator<Int, HomeStatusDatabaseEntity>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, HomeStatusDatabaseEntity>): MediatorResult {

        val (max_id, min_id) = when (loadType) {
            LoadType.REFRESH -> {
                Pair<String?, String?>(null, null)
            }
            LoadType.PREPEND -> {
                //No prepend for the moment, might be nice to add later
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            LoadType.APPEND -> {
                Pair<String?, String?>(state.lastItemOrNull()?.id, null)
            }

        }

        try {
            val user = db.userDao().getActiveUser()
                    ?: return MediatorResult.Error(NullPointerException("No active user exists"))
            val api = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)
            val accessToken = user.accessToken

            val apiResponse = api.timelineHome(
                max_id= max_id,
                min_id = min_id, limit = state.config.pageSize.toString()
            )

            val dbObjects = apiResponse.map{
                HomeStatusDatabaseEntity(user.user_id, user.instance_uri, it)
            }

            val endOfPaginationReached = apiResponse.isEmpty()

            db.withTransaction {
                // clear table in the database
                if (loadType == LoadType.REFRESH) {
                    db.homePostDao().clearFeedContent()
                }
                db.homePostDao().insertAll(dbObjects)
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }
}