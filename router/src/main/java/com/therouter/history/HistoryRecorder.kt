@file:JvmName("HistoryRecorder")

package com.therouter.history

import java.util.*
import kotlin.collections.ArrayList

private val history = LinkedList<History>()

fun pushHistory(event: History) = history.addLast(event)

fun popHistory() = history.removeLast()

fun export(level: Level): List<String> {
    val list = ArrayList<String>()
    ArrayList(history).forEach {
        when (it) {
            is ActivityNavigatorHistory -> {
                if (level.v.and(Level.ACTIVITY.v) == Level.ACTIVITY.v) {
                    list.add(it.event)
                }
            }
            is FragmentNavigatorHistory -> {
                if (level.v.and(Level.FRAGMENT.v) == Level.FRAGMENT.v) {
                    list.add(it.event)
                }
            }
            is ActionNavigatorHistory -> {
                if (level.v.and(Level.ACTION.v) == Level.ACTION.v) {
                    list.add(it.event)
                }
            }
            is ServiceProviderHistory -> {
                if (level.v.and(Level.SERVICE_PROVIDER.v) == Level.SERVICE_PROVIDER.v) {
                    list.add(it.event)
                }
            }
            is FlowTaskHistory -> {
                if (level.v.and(Level.FLOW_TASK.v) == Level.FLOW_TASK.v) {
                    list.add(it.event)
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
