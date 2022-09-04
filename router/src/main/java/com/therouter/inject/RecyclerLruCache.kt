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

class RecyclerLruCache(maxSize: Int) : LruCache<ClassWrapper<*>?, Any?>(maxSize) {
    private var mListener: (key: ClassWrapper<*>?, oldValue: Any?, newValue: Any?) -> Unit = { _, _, _ ->
    }

    override fun entryRemoved(evicted: Boolean, key: ClassWrapper<*>?, oldValue: Any?, newValue: Any?) {
        super.entryRemoved(evicted, key, oldValue, newValue)
        mListener(key, oldValue, newValue)
    }

    fun setOnEntryRemovedListener(listener: OnEntryRemovedListener?) {
        listener?.let {
            mListener = it::entryRemoved
        }
    }

    fun setOnEntryRemovedListener(block: (key: ClassWrapper<*>?, oldValue: Any?, newValue: Any?) -> Unit) {
        mListener = block
    }

    interface OnEntryRemovedListener {
        fun entryRemoved(key: ClassWrapper<*>?, oldValue: Any?, newValue: Any?)
    }
}