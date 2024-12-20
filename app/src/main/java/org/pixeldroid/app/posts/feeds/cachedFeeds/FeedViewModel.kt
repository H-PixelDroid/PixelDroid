/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pixeldroid.app.posts.feeds.cachedFeeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import org.pixeldroid.app.utils.api.objects.FeedContentDatabase
import kotlinx.coroutines.flow.Flow

/**
 * ViewModel for the cached feeds.
 * The ViewModel works with the [FeedContentRepository] to get the data.
 */
class FeedViewModel<T: FeedContentDatabase>(repository: FeedContentRepository<T>) : ViewModel() {

    @ExperimentalPagingApi
    val flow: Flow<PagingData<T>> = repository.stream().cachedIn(viewModelScope)
}
