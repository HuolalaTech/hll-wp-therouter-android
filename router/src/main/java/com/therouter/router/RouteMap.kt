package com.therouter.router

import a.initDefaultRouteMap
import android.content.Intent
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.therouter.ROUTE_MAP_ASSETS_PATH
import com.therouter.TheRouter
import com.therouter.debug
import com.therouter.execute
import com.therouter.executeInMainThread
import com.therouter.getApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

// 路由表
private val ROUTER_MAP = RegexpKeyedMap<RouteItem>()
private var initTask: RouterMapInitTask? = null

@Volatile
internal var initedRouteMap = false
private var onRouteMapChangedListener: OnRouteMapChangedListener? = null
val gson = Gson()

/**
 * 在主线程初始化路由表
 */
fun initRouteMap() {
    try {
        InputStreamReader(
            getStreamFromAssets(getApplicationContext(), ROUTE_MAP_ASSETS_PATH), Charset.forName("UTF-8")
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
        debug("RouteMap", "initRouteMap InputStreamReader error") {
            e.printStackTrace()
        }
    }
}

/**
 * 在异步初始化路由表
 */
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

/**
 * 设置自定义路由表初始化逻辑，设置后会在异步线程中调用自定义初始化逻辑
 * 默认初始化逻辑为 initRouteMap()
 */
fun setRouteMapInitTask(task: RouterMapInitTask?) = task?.let {
    initTask = task
}

/**
 * 当路由项被改变时，会触发回调。例如路由项被覆盖、路由项内容发生变化
 */
fun setOnRouteMapChangedListener(listener: OnRouteMapChangedListener?) {
    // 允许被空覆盖
    onRouteMapChangedListener = listener
}

/**
 * 设置自定义路由表初始化逻辑，设置后会在异步线程中调用自定义初始化逻辑
 * 默认初始化逻辑为 initRouteMap()
 */
fun setRouteMapInitTask(task: () -> Unit) {
    initTask = object : RouterMapInitTask {
        override fun asyncInitRouteMap() {
            task.invoke()
        }
    }
}

/**
 * 尝试通过Intent，从路由表中获取对应的路由Path
 * 如果路由表中没有对应路由，则将类名作为路由Path新创建一条路由项
 */
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

/**
 * 尝试通过Path，从路由表中获取对应的路由项，如果没有对应路由，则返回null
 */
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

/**
 * 尝试通过ClassName，从路由表中获取对应的路由项，如果没有对应路由，则返回空数组
 */
@Synchronized
fun matchRouteMapForClassName(className: String?): List<RouteItem> {
    val result = ArrayList<RouteItem>()
    ROUTER_MAP.values.forEach {
        it?.let {
            if (it.className == className) {
                // copy是为了防止外部修改影响路由表
                result.add(it.copy())
            }
        }
    }
    return result
}

/**
 * 向路由表添加路由
 */
@Synchronized
fun addRouteMap(routeItemArray: Collection<RouteItem>?) {
    if (routeItemArray != null && !routeItemArray.isEmpty()) {
        for (entity in routeItemArray) {
            addRouteItem(entity)
        }
    }
}

/**
 * 向路由表添加路由
 */
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