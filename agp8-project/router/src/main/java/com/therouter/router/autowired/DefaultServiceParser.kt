package com.therouter.router.autowired

import com.therouter.TheRouter
import com.therouter.router.AutowiredItem
import com.therouter.router.interceptor.AutowiredParser

class DefaultServiceParser : AutowiredParser {
    override fun <T> parse(type: String?, target: Any?, item: AutowiredItem?): T? {
        if (item != null && item.id == 0) {
            try {
                (Class.forName(item.type) as? Class<T>)?.let { return@let TheRouter.get(it) }
            } catch (e: Exception) {
            }
        }
        return null
    }
}