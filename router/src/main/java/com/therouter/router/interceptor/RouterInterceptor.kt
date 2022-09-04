package com.therouter.router.interceptor

import com.therouter.router.RouteItem

interface RouterInterceptor {
    //callback.onContinue(routerItem);
    fun process(routeItem: RouteItem, callback: InterceptorCallback)
}

interface InterceptorCallback {
    fun onContinue(routeItem: RouteItem)
}