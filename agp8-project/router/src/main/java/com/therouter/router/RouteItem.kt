package com.therouter.router

import android.os.Bundle
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat.getExtras
import com.therouter.router.interceptor.NavigatorParamsFixHandle
import java.io.Serializable
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*

/**
 * 独立的一条路由记录
 *
 * @param path 路由Path
 * @param className 落地页类名
 * @param action 跳转到落地页以后，需要执行的动作
 * @param description 当前路由的注释
 * @param params 仅用于RouteMap.json文件被gson转换时存储，外部不可访问，会被合并到extras中
 * @param extras extras存储运行期的路由表参数
 *
 */
@Keep
class RouteItem : Serializable {
    var path = ""
    var className = ""
    var action = ""
    var description = ""

    // 仅用于RouteMap.json文件被gson转换时存储，外部不可访问
    val params = HashMap<String, String>()

    // extras存储运行期的路由表参数
    private val extras = Bundle()

    constructor()

    constructor(path: String, className: String, action: String, description: String) {
        this.path = path
        this.className = className
        this.action = action
        this.description = description
    }

    fun addParams(key: String, value: String) {
        extras.putString(key, value)
    }

    internal fun addAll(bundle: Bundle?) = bundle?.let { extras.putAll(it) }

    fun getExtras(): Bundle {
        params.forEach {
            // extras为运行期代码逻辑产生的动态参数，优先级最高，不允许被路由表静态参数覆盖
            if (!extras.keySet().contains(it.key)) {
                extras.putString(it.key, it.value)
            }
        }
        return extras
    }

    override fun toString(): String {
        return "RouteItem(path='$path', className='$className', action='$action', description='$description', extras=$extras)"
    }

    fun copy(): RouteItem {
        val item = RouteItem()
        item.extras.putAll(extras)
        item.params.putAll(params)
        item.description = description
        item.action = action
        item.className = className
        item.path = path
        return item
    }
}

/**
 * 将当前路由转换为导航器
 */
fun RouteItem.toNavigator() = Navigator(getUrlWithParams(), null)

/**
 * 获取当前路由的完整url记录
 */
fun RouteItem.getUrlWithParams() = getUrlWithParams { k, v -> "$k=$v" }

/**
 * 获取当前路由的完整url记录
 */
fun RouteItem.getUrlWithParams(handle: NavigatorParamsFixHandle) = getUrlWithParams(handle::fix)

/**
 * 获取当前路由的完整url记录
 */
fun RouteItem.getUrlWithParams(handle: (String, String) -> String): String {
    val stringBuilder = StringBuilder(path)
    var isFirst = true
    val extras = getExtras()
    for (key in extras.keySet()) {
        if (isFirst) {
            stringBuilder.append("?")
            isFirst = false
        } else {
            stringBuilder.append("&")
        }
        stringBuilder.append(handle(key, extras.get(key)?.toString() ?: ""))
    }
    return stringBuilder.toString()
}
