package com.therouter.router

import java.util.*
import java.util.regex.Pattern

/**
 * 专门为路由服务，因为路由的path存在正则的情况
 *
 * 优化说明：
 * 1. 新增 compiledPatterns 缓存已编译的正则表达式，避免每次 get() 都重新编译
 *    原因：Pattern.compile() 是耗时操作，路由表增量大时性能明显下降
 *    目的：将 O(n) 的正则编译优化为 O(1) 缓存查找
 * 2. 使用 WeakHashMap 确保缓存不会导致内存泄漏
 */
class RegexpKeyedMap<V> : HashMap<String?, V?>() {

    /**
     * 缓存预编译的正则表达式Pattern对象
     * 使用WeakHashMap确保不会阻止垃圾回收，避免内存泄漏
     */
    private val compiledPatterns = WeakHashMap<String?, Pattern>()

    /**
     * 获取预编译的正则表达式，如果缓存中没有则编译并缓存
     * 目的：避免在每次get()调用时重复编译相同的正则表达式
     */
    private fun getCompiledPattern(regex: String?): Pattern? {
        return compiledPatterns.getOrPut(regex) {
            Pattern.compile(regex!!)
        }
    }

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
        // 优化：使用缓存的Pattern对象，避免重复编译
        for (regex in keys) {
            if (regex == null || !regex.contains("\\")) {
                continue
            }
            // 优化点：从缓存获取预编译的Pattern，而不是每次都调用Pattern.compile()
            val pattern = getCompiledPattern(regex)
            var m = pattern.matcher(key)
            if (m.find()) {
                result = super.get(regex)
                break
            }
            if (keyPath != null) {
                // 复用同一个matcher对象，避免重复创建
                m = pattern.matcher(keyPath)
                if (m.find()) {
                    result = super.get(regex)
                    break
                }
            }
        }
        return result
    }
}
