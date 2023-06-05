package com.therouter.router.interceptor

import com.therouter.router.RouteItem

/**
 * 路由替换器
 * 应用场景：常用在未登录不能使用的页面上。例如访问用户钱包页面，在钱包页声明的时候，可以在路由表上声明本页面是需要登录的，在路由跳转过程中，如果落地页是需要登录的，则先替换路由到登录页，同时将原落地页信息作为参数传给登录页，登录流程处理完成后可以继续执行之前的路由操作。
 *
 * 路由替换器的拦截点更靠后，主要用于框架已经从路由表中根据 path 找到路由以后，对找到的路由做操作。
 *
 * 这种逻辑在所有页面跳转前写不太合适，以前的做法通常是在落地页写逻辑判断用户是否具有权限，但其实在路由层完成更合适。
 * 注：必须在 TheRouter.build().navigation() 方法调用前添加处理器，否则处理器前的所有跳转不会被替换。
 */
abstract class RouterReplaceInterceptor {
    open fun watch(routeItem: RouteItem?): Boolean = true

    abstract fun replace(routeItem: RouteItem?): RouteItem?

    /**
     * 数字越大，优先级越高
     *
     * @return
     */
    open val priority: Int
        get() = 5
}