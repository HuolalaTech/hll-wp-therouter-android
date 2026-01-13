package com.therouter.compose

import androidx.compose.runtime.Composable
import com.therouter.app.brick.DataRepository.brickRouteMap
import com.therouter.router.Navigator
import com.therouter.router.arguments
import java.lang.ref.SoftReference
import kotlin.collections.forEach

internal val routeMap = HashMap<String, @Composable (Map<String, Any?>?) -> Unit>()

//vararg params: Any?
fun composable(route: String, composable: @Composable (Map<String, Any?>?) -> Unit) {
    routeMap.put(route, composable)
}

fun Navigator.withComposableObject(key: String, value: @Composable () -> Unit): Navigator {
    arguments[key] = SoftReference(value)
    return this
}

@Composable
fun Navigator.compose() {
    val type = simpleUrl
    val allField = brickRouteMap.get(type)
    allField?.forEach {
        val fieldName = it.key
        val priorityList = it.value

        priorityList.sortBy { a -> a.priority }
        if (priorityList.isNotEmpty()) {
            priorityList.forEach { provider ->
                val t = provider.make?.invoke(this)
                if (t != null) {
                    this.withObject(fieldName, t)
                    return@forEach
                }
            }
        }
    }

    val params = HashMap<String, Any?>()
    params.putAll(this.kvPair)
    this.extras.keySet().forEach {
        params[it] = this.extras.get(it)
    }
    arguments.keys.forEach {
        params[it] = arguments.get(it)?.get()
    }
    arguments.clear()
    routeMap[this.simpleUrl]?.invoke(params)
}
