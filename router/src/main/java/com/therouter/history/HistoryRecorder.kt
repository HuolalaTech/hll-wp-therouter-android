@file:JvmName("HistoryRecorder")

package com.therouter.history

import com.therouter.inject.RecyclerLruCache
import java.util.*
import kotlin.collections.ArrayList

private var counter: Long = 0

var HISTORY_LOG_MAX_SIZE = 30

private val mCacher = RecyclerLruCache<String?, History?>(HISTORY_LOG_MAX_SIZE).apply {
    setOnEntryRemovedListener { key, oldValue, _ -> m2ndCacher[key] = oldValue }
}

private val m2ndCacher = WeakHashMap<String?, History?>()

@Synchronized
fun pushHistory(event: History) = mCacher.put("${counter++}", event)

/**
 * 导出路由的全部记录
 */
@Synchronized
fun export(level: Level): List<String> {
    val list = ArrayList<String>()
    for (index in 0..counter) {
        val item = mCacher.get("$index") ?: m2ndCacher["$index"]
        item?.let { history ->
            when (history) {
                is ActivityNavigatorHistory -> {
                    if (level.v.and(Level.ACTIVITY.v) == Level.ACTIVITY.v) {
                        list.add(history.event)
                    }
                }

                is FragmentNavigatorHistory -> {
                    if (level.v.and(Level.FRAGMENT.v) == Level.FRAGMENT.v) {
                        list.add(history.event)
                    }
                }

                is ActionNavigatorHistory -> {
                    if (level.v.and(Level.ACTION.v) == Level.ACTION.v) {
                        list.add(history.event)
                    }
                }

                is ServiceProviderHistory -> {
                    if (level.v.and(Level.SERVICE_PROVIDER.v) == Level.SERVICE_PROVIDER.v) {
                        list.add(history.event)
                    }
                }

                is FlowTaskHistory -> {
                    if (level.v.and(Level.FLOW_TASK.v) == Level.FLOW_TASK.v) {
                        list.add(history.event)
                    }
                }
            }
        }
    }
    return list
}

interface History
class ActivityNavigatorHistory(val event: String) : History
class FragmentNavigatorHistory(val event: String) : History
class ActionNavigatorHistory(val event: String) : History
class ServiceProviderHistory(val event: String) : History
class FlowTaskHistory(val event: String) : History

open class Level {
    var v: Int = 0
        private set

    /**
     * 仅类内部使用，写起来代码简洁一点
     */
    private fun sv(value: Int): Level {
        v = value
        return this
    }

    companion object {
        val NONE = Level().sv(0x000000)
        val ACTIVITY = Level().sv(0x000001)
        val FRAGMENT = Level().sv(0x000010)
        val PAGE = Level().sv(0x000011)
        val ACTION = Level().sv(0x001000)
        val SERVICE_PROVIDER = Level().sv(0x010000)
        val FLOW_TASK = Level().sv(0x100000)
        val ALL = Level().sv(0x111111)
    }

    operator fun plus(o: Level): Level {
        return Level().sv(o.v or v)
    }

    operator fun minus(o: Level): Level {
        return Level().sv(o.v xor v)
    }
}
