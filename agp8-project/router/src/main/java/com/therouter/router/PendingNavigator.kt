package com.therouter.router

/**
 * 可以被挂起的导航器，通常用于延迟动作。
 * 延迟跳转主要应用场景有两种：
 * 第一种：初始化时期，如果路由表的量非常巨大时。这种情况在别的路由框架上要么会白屏一段时间，要么直接丢弃这次跳转。在TheRouter中，框架会暂存当前的跳转动作，在路由表初始化完成后立刻执行跳转。
 * 第二种：从Android 8.0开始，Activity 不能在后台启动页面，这对于业务判断造成了很大的影响。由于可能会有前台 Service 的情况，不能单纯以 Activity 生命周期判断前后台。在TheRouter中，框架允许业务自定义前后台规则，如果为后台情况，可以将跳转动作暂存，当进入前台后再恢复跳转。
 *
 * 恢复跳转时需要调用
 * sendPendingNavigator(); //toplevel方法，无需类名调用，Java请通过NavigatorKt类名调用
 *
 */
class PendingNavigator(val navigator: Navigator, val action: () -> Unit) {
    override fun equals(other: Any?): Boolean {
        if (other is PendingNavigator) {
            return other.navigator == navigator
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return navigator.hashCode() + 1
    }
}
