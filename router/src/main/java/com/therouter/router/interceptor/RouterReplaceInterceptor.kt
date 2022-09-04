package com.therouter.router.interceptor

import com.therouter.router.RouteItem

abstract class RouterReplaceInterceptor {
    abstract fun replace(routeItem: RouteItem?): RouteItem?

    /**
     * 数字越大，优先级越高
     *
     * @return
     */
    open val priority: Int
        get() = 5
}