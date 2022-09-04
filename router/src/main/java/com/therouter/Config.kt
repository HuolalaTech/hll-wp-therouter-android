@file:JvmName("Config")

package com.therouter

var WEB_HOST = "therouter.com"
var WEB_SCHEME = "https"
var ROUTE_MAP_ASSETS_PATH = "therouter/routeMap.json"

/**
 * 可选工具类，将路由path的相对路径转为绝对路径
 */
fun fixPath(url: String): String {
    var url = url
    url = if (url.startsWith("http")) {
        // 什么也不做
        url
    } else if (url.startsWith("//")) {
        "$WEB_SCHEME:$url"
    } else if (url.startsWith("/")) {
        if (url.startsWith("/$WEB_HOST")) {
            "$WEB_SCHEME:/$url"
        } else {
            "$WEB_SCHEME://$WEB_HOST$url"
        }
    } else {
        if (url.startsWith(WEB_HOST)) {
            "$WEB_SCHEME://$url"
        } else {
            "$WEB_SCHEME://$WEB_HOST/$url"
        }
    }
    return url
}