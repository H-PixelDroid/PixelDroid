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

package com.h.pixeldroid.posts.feeds.cachedFeeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.h.pixeldroid.utils.api.objects.FeedContentDatabase
import kotlinx.coroutines.flow.Flow

/**
 * ViewModel for the cached feeds.
 * The ViewModel works with the [FeedContentRepository] to get the data.
 */
class FeedViewModel<T: FeedContentDatabase>(private val repository: FeedContentRepository<T>) : ViewModel() {

    private var currentResult: Flow<PagingData<T>>? = null

    @ExperimentalPagingApi
    fun flow(): Flow<PagingData<T>> {
        val lastResult = currentResult
        if (lastResult != null) {
            return lastResult
        }
        val newResult: Flow<PagingData<T>> = repository.stream()
                .cachedIn(viewModelScope)
        currentResult = newResult
        return newResult
    }
}
