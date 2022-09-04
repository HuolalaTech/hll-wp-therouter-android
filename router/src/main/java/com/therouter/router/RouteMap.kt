package com.therouter.router

import a.initDefaultRouteMap
import android.content.Intent
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.therouter.*
import com.therouter.debug
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.nio.charset.StandardCharsets

// 路由表
private val ROUTER_MAP = RegexpKeyedMap<RouteItem>()
private var initTask: RouterMapInitTask? = null

@Volatile
internal var initedRouteMap = false
private var onRouteMapChangedListener: OnRouteMapChangedListener? = null
val gson = Gson()


fun initRouteMap() {
    try {
        InputStreamReader(
            getStreamFromAssets(getApplicationContext(), ROUTE_MAP_ASSETS_PATH), StandardCharsets.UTF_8
        ).use {
            BufferedReader(it).use { read ->
                var lineText: String?
                val stringBuilder = StringBuilder()
                while (read.readLine().also { lineText = it } != null) {
                    stringBuilder.append(lineText).append("\n")
                }
                val content = stringBuilder.toString()
                debug("RouteMap", "will be add route map from assets: $content")
                if (!TextUtils.isEmpty(content)) {
                    val list: List<RouteItem> =
                        gson.fromJson(content, object : TypeToken<List<RouteItem?>?>() {}.getType())
                    addRouteMap(list)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun asyncInitRouteMap() {
    execute {
        debug("RouteMap", "will be add route map from： initDefaultRouteMap()")
        initDefaultRouteMap()
        initedRouteMap = true
        if (initTask == null) {
            initRouteMap()
        } else {
            debug("RouteMap", "will be add route map from： RouterMapInitTask")
            initTask?.asyncInitRouteMap()
        }
        executeInMainThread {
            sendPendingNavigator()
        }
    }
}

fun setRouteMapInitTask(task: RouterMapInitTask?) = task?.let {
    initTask = task
}

fun setOnRouteMapChangedListener(listener: OnRouteMapChangedListener?) {
    // 允许被空覆盖
    onRouteMapChangedListener = listener
}

fun setRouteMapInitTask(task: () -> Unit) {
    initTask = object : RouterMapInitTask {
        override fun asyncInitRouteMap() {
            task.invoke()
        }
    }
}

@Synchronized
fun foundPathFromIntent(intent: Intent): String? {
    val className = intent.component?.className
    className?.let {
        ROUTER_MAP.values.forEach {
            if (it?.className == className) {
                return it.path
            }
        }
        // 如果路由表中没有这个类名，则新增一条路由项
        val item = RouteItem(className, className, "", className)
        item.addAll(intent.extras)
        addRouteItem(item)
        return className
    }
    return null
}

@Synchronized
fun matchRouteMap(url: String?): RouteItem? {
    var path = TheRouter.build(url ?: "").simpleUrl
    if (path.endsWith("/")) {
        path = path.substring(0, path.length - 1)
    }
    // copy是为了防止外部修改影响路由表
    val routeItem = ROUTER_MAP[path]?.copy()
    // 由于路由表中的path可能是正则path，要用入参替换掉
    routeItem?.path = path
    return routeItem
}

@Synchronized
fun addRouteMap(routeItemArray: Collection<RouteItem>?) {
    if (routeItemArray != null && !routeItemArray.isEmpty()) {
        for (entity in routeItemArray) {
            addRouteItem(entity)
        }
    }
}

@Synchronized
fun addRouteItem(routeItem: RouteItem) {
    var path = routeItem.path
    if (path.endsWith("/")) {
        path = path.substring(0, path.length - 1)
    }
    debug("addRouteItem", "add $path")
    ROUTER_MAP[path] = routeItem
    onRouteMapChangedListener?.onChanged(routeItem)
}