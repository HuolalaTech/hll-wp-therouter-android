package com.therouter.router.interceptor

/**
 * 应用场景：用于修复客户端上路由 path 错误问题。
 * 例如：相对路径转绝对路径，或由于服务端下发的链接无法固定https或http，但客户端代码写死了 https 的 path，就可以用这种方式统一。
 * 注：必须在 TheRouter.build() 方法调用前添加处理器，否则处理器前的所有path不会被修改。
 */
abstract class NavigatorPathFixHandle {

    open fun watch(path: String?): Boolean = true

    abstract fun fix(path: String?): String?

    /**
     * 数字越大，优先级越高
     *
     * @return
     */
    open val priority: Int
        get() = 5
}