package com.therouter.router

import java.util.*
import java.util.regex.Pattern

/**
 * 专门为路由服务，因为路由的path存在正则的情况
 */
class RegexpKeyedMap<V> : HashMap<String?, V?>() {

    override fun get(key: String?): V? {
        if (key == null) return null

        // 直接查找
        var result = super.get(key)
        if (result != null) {
            return result
        }
        var keyPath: String? = null
        val index = key.indexOf('?')
        if (index > 0) {
            keyPath = key.substring(0, index)
            result = super.get(keyPath)
            if (result != null) {
                return result
            }
        }

        // 找不到，则进行正则匹配
        for (regex in keys) {
            if (regex == null || !regex.contains("\\")) {
                continue
            }
            val r = Pattern.compile(regex)
            val m = r.matcher(key)
            if (m.find()) {
                result = super.get(regex)
                break
            }
            if (keyPath != null) {
                val m2 = r.matcher(key)
                if (m2.find()) {
                    result = super.get(regex)
                    break
                }
            }
        }
        return result
    }
}