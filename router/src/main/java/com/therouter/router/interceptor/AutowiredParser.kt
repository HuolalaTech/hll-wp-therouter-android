package com.therouter.router.interceptor

import com.therouter.router.AutowiredItem

interface AutowiredParser {
    fun <T> parse(type: String?, target: Any?, item: AutowiredItem?): T?
}