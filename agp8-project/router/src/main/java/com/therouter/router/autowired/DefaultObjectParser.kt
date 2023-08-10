package com.therouter.router.autowired

import com.therouter.router.AutowiredItem
import com.therouter.router.arguments
import com.therouter.router.interceptor.AutowiredParser

class DefaultObjectParser : AutowiredParser {
    override fun <T> parse(type: String?, target: Any?, item: AutowiredItem?): T? {
        if (item != null && item.id == 0) {
            if (arguments.keys.contains(item.key)) {
                return arguments.remove(item.key)?.get() as T?
            }
        }
        return null
    }
}