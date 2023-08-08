package com.therouter.router.interceptor

/**
 * 页面替换器
 * 应用场景：需要将某些path指定为新链接的时候使用。 也可以用在修复链接的场景，但是与 path 修改器不同的是，修改器通常是为了解决通用性的问题，替换器只在页面跳转时才会生效，更多是用来解决特性问题。
 *
 * 例如模块化的时候，首页壳模板组件中开发了一个SplashActivity广告组件作为应用的MainActivity，在闪屏广告结束的时候自动跳转业务首页页面。 但是每个业务不同，首页页面的 Path 也不相同，而不希望让每个业务线自己去改这个首页壳模板组件，此时就可以组件中先写占位符https://kymjs.com/splash/to/home，让接入方通过 Path 替换器解决。
 * 注：必须在 TheRouter.build().navigation() 方法调用前添加处理器，否则处理器前的所有跳转不会被替换。
 */
abstract class PathReplaceInterceptor {

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