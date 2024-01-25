package com.therouter.router.interceptor

/**
 * 路由匹配拦截器
 * 应用场景：拦截路由表遍历。
 *
 * 例如需要打开H5页面的时候，通常的做法是TheRouter.build(h5_path).with("url","https://kymjs.com").navigation()。
 * 此时可以定制路由表匹配规则，将所有https://开头的path都认为是由h5_path所代表的页面打开。
 * 这样代码就可以写成TheRouter.build("https://kymjs.com").navigation()。
 * 该拦截器同时影响TheRouter.isRouterPath()
 * 注：必须在 TheRouter.build().navigation() 方法调用前添加处理器，否则处理器前的所有跳转不会被替换。
 */
abstract class MatchInterceptor {

    open fun watch(path: String?): Boolean = true

    abstract fun replace(path: String?): String?

    /**
     * 数字越大，优先级越高
     *
     * @return
     */
    open val priority: Int
        get() = 5
}