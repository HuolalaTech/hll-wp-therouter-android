/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.therouter.inject

import android.util.LruCache

class RecyclerLruCache<K, V>(maxSize: Int) : LruCache<K, V>(maxSize) {
    private var mListener: (key: K, oldValue: V, newValue: V) -> Unit = { _, _, _ ->
    }

    override fun entryRemoved(evicted: Boolean, key: K, oldValue: V, newValue: V) {
        super.entryRemoved(evicted, key, oldValue, newValue)
        mListener(key, oldValue, newValue)
    }

    fun setOnEntryRemovedListener(listener: OnEntryRemovedListener<K, V>?) {
        listener?.let {
            mListener = it::entryRemoved
        }
    }

    fun setOnEntryRemovedListener(block: (key: K, oldValue: V, newValue: V) -> Unit) {
        mListener = block
    }

    interface OnEntryRemovedListener<K, V> {
        fun entryRemoved(key: K, oldValue: V, newValue: V)
    }
}