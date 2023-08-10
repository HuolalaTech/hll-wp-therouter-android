package com.therouter.router.interceptor

import com.therouter.router.RouteItem

/**
 * 路由AOP拦截器
 * 与前三个处理器不同的点在于，路由的AOP拦截器全局只能有一个。用于实现AOP的能力，在整个TheRouter跳转的过程中，跳转前，目标页是否找到的回调，跳转时，跳转后，都可以做一些自定义的逻辑处理。
 *
 * 使用场景：场景很多，最常用的是可以拦截一些跳转，例如debug页面在生产环境不打开，或定制startActivity跳转方法。
 */
interface RouterInterceptor {
    //callback.onContinue(routerItem);
    fun process(routeItem: RouteItem, callback: InterceptorCallback)
}

/**
 * 路由AOP拦截器
 * 与前三个处理器不同的点在于，路由的AOP拦截器全局只能有一个。用于实现AOP的能力，在整个TheRouter跳转的过程中，跳转前，目标页是否找到的回调，跳转时，跳转后，都可以做一些自定义的逻辑处理。
 *
 * 使用场景：场景很多，最常用的是可以拦截一些跳转，例如debug页面在生产环境不打开，或定制startActivity跳转方法。
 */
interface InterceptorCallback {
    fun onContinue(routeItem: RouteItem)
}