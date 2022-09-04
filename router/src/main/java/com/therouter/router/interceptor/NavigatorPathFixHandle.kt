package com.therouter.router.interceptor

abstract class NavigatorPathFixHandle {
    abstract fun fix(path: String?): String?

    /**
     * 数字越大，优先级越高
     *
     * @return
     */
    open val priority: Int
        get() = 5
}