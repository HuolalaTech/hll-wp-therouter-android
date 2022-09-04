package com.therouter.router.action

import android.content.Context
import android.text.TextUtils
import com.therouter.debug
import com.therouter.router.action.interceptor.ActionInterceptor
import com.therouter.getApplicationContext
import com.therouter.history.ActionNavigatorHistory
import com.therouter.history.pushHistory
import com.therouter.router.Navigator
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

internal object ActionManager {
    // simpleUrl - runnable
    private val actionHandleMap: HashMap<String, MutableList<ActionInterceptor?>> = HashMap()

    internal fun isAction(navigator: Navigator) = actionHandleMap[navigator.simpleUrl] != null

    internal fun handleAction(navigator: Navigator, context: Context?) {
        if (TextUtils.isEmpty(navigator.simpleUrl)) return
        debug("ActionManager", "handleAction->${navigator.urlWithParams}") {
            for (traceElement in Thread.currentThread().stackTrace) {
                debug("ActionManager", "$traceElement")
            }
        }

        val list = ArrayList<ActionInterceptor>()
        val interceptorList = actionHandleMap[navigator.simpleUrl]
        if (interceptorList != null) {
            for (item in interceptorList) {
                if (item == null) continue
                pushHistory(ActionNavigatorHistory(navigator.urlWithParams))
                val bool = item.handle(context ?: getApplicationContext()!!, navigator.extras)
                list.add(item)
                if (bool) {
                    break
                }
            }
        }

        list.forEach {
            it.onFinish()
        }
    }

    internal fun addActionInterceptor(action: String?, interceptor: ActionInterceptor?) {
        if (TextUtils.isEmpty(action)) return
        val realAction = Navigator(action).simpleUrl
        var actionList = actionHandleMap[realAction]
        if (actionList == null) {
            actionList = ArrayList<ActionInterceptor?>()
        }
        if (!actionList.contains(interceptor)) {
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