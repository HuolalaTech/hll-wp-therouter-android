package com.therouter.router.interceptor

import android.app.Activity
import com.therouter.router.Navigator

/**
 * 自定义全局路由跳转结果回调
 *
 * 如果使用TheRouter跳转，传入了一个不识别的的path，则不会有任何处理。你也可以定义一个默认的全局回调，来处理跳转情况，如果落地页是 Fragment 则不会回调。
 * 当然，跳转结果的回调不止这一个用途，可以根据业务有自己的处理。
 * 回调也可以单独为某一次跳转设置，navigation()方法有重载可以传入设置。
 */
open class NavigationCallback {
    open fun onFound(navigator: Navigator) {}
    open fun onLost(navigator: Navigator) {}
    open fun onArrival(navigator: Navigator) {}
    open fun onActivityCreated(navigator: Navigator, activity: Activity) {}
}