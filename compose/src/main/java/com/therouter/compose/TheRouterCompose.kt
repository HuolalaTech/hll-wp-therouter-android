package com.therouter.compose

import androidx.compose.runtime.Composable
import com.therouter.router.Navigator
import com.therouter.router.arguments
import java.lang.ref.SoftReference

internal val routeMap = HashMap<String, @Composable (Map<String, Any?>?) -> Unit>()

//vararg params: Any?
fun composable(route: String, composable: @Composable (Map<String, Any?>?) -> Unit) {
    routeMap.put(route, composable)
}

fun Navigator.withComposableObject(key: String, value: @Composable Any): Navigator {
    arguments[key] = SoftReference(value)
    return this
}

@Composable
fun Navigator.compose() {
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