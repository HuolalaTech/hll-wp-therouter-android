package com.therouter

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

private var applicationContext: Context? = null

fun getApplicationContext() = applicationContext

fun setContext(c: Context?) = c?.let {
    applicationContext = it
}

class InnerTheRouterContentProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        applicationContext ?: let {
            applicationContext = context
        }
        if (theRouterUseAutoInit) {
            TheRouter.init(applicationContext)
            debug("InnerTheRouterContentProvider", "TheRouter auto init in Application")
        }
        return true
    }

    override fun query(uri: Uri, strings: Array<String>?, s: String?, strings1: Array<String>?, s1: String?): Cursor? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, s: String?, strings: Array<String>?): Int {
        return 0
    }

    override fun update(uri: Uri, contentValues: ContentValues?, s: String?, strings: Array<String>?): Int {
        return 0
    }
}