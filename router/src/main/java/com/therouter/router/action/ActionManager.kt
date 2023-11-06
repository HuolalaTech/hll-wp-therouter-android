package com.therouter.router.action

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import com.therouter.debug
import com.therouter.router.action.interceptor.ActionInterceptor
import com.therouter.getApplicationContext
import com.therouter.history.ActionNavigatorHistory
import com.therouter.history.pushHistory
import com.therouter.router.Navigator
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.Comparator
import kotlin.collections.ArrayList

internal object ActionManager {
    // simpleUrl - runnable
    private val actionHandleMap = ConcurrentHashMap<String, MutableList<ActionInterceptor?>>()

    internal fun isAction(navigator: Navigator) = actionHandleMap[navigator.simpleUrl] != null

    @Synchronized
    internal fun handleAction(navigator: Navigator, context: Context?) {
        if (TextUtils.isEmpty(navigator.simpleUrl)) return
        debug("ActionManager", "handleAction->${navigator.getUrlWithParams()}") {
            for (traceElement in Thread.currentThread().stackTrace) {
                debug("ActionManager", "$traceElement")
            }
        }

        val list = ArrayList<ActionInterceptor>()
        val interceptorList = actionHandleMap[navigator.simpleUrl]?.let { CopyOnWriteArrayList(it) }
        var bundle = Bundle()
        if (interceptorList != null) {
            for (item in interceptorList) {
                if (item == null) continue
                item.setArguments(bundle)
                pushHistory(ActionNavigatorHistory(navigator.getUrlWithParams()))
                val bool = item.handle(context ?: getApplicationContext()!!, navigator)
                bundle = item.getArguments()
                list.add(item)
                if (bool) {
                    break
                }
            }
        }

        list.forEach {
            it.setArguments(bundle)
            it.onFinish()
        }
    }

    @Synchronized
    internal fun addActionInterceptor(action: String?, interceptor: ActionInterceptor?) {
        if (TextUtils.isEmpty(action)) return
        val realAction = Navigator(action).simpleUrl
        var actionList = actionHandleMap[realAction]
        if (actionList == null) {
            actionList = Collections.synchronizedList(ArrayList())
        }
        if (actionList != null && !actionList.contains(interceptor)) {
            actionList.add(interceptor)
            Collections.sort(actionList, Comparator { o1, o2 ->
                return@Comparator if (o1 == null) {
                    -1
                } else if (o2 == null) {
                    1
                } else o2.priority - o1.priority
            })
            actionHandleMap[realAction] = actionList
        }
    }

    /**
     * 移除所有action对应的拦截器，如果action有多个拦截器，则都会被移除
     */
    @Synchronized
    internal fun removeAllInterceptorForKey(action: String?): MutableList<ActionInterceptor?> {
        if (TextUtils.isEmpty(action)) return ArrayList()
        val realAction = Navigator(action).simpleUrl
        return actionHandleMap.remove(realAction) ?: ArrayList()
    }

    /**
     * 移除所有指定拦截器，如果有多个action共用同一个拦截器，则都会被移除
     */
    @Synchronized
    internal fun removeAllInterceptorForValue(interceptor: ActionInterceptor?) {
        actionHandleMap.keys.forEach { k ->
            val actionList = actionHandleMap[k]
            if (actionList != null) {
                actionList.remove(interceptor)
                actionHandleMap[k] = actionList
            }
        }
    }

    @Synchronized
    internal fun removeActionInterceptor(action: String?, interceptor: ActionInterceptor?) {
        if (TextUtils.isEmpty(action)) return
        val realAction = Navigator(action).simpleUrl
        val actionList = actionHandleMap[realAction]
        if (actionList != null) {
            actionList.remove(interceptor)
            actionHandleMap[realAction] = actionList
        }
    }
}