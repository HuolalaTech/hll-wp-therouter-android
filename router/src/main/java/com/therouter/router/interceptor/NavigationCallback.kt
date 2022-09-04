package com.therouter.router.interceptor

import android.app.Activity
import com.therouter.router.Navigator

open class NavigationCallback {
    open fun onFound(navigator: Navigator) {}
    open fun onLost(navigator: Navigator) {}
    open fun onArrival(navigator: Navigator) {}
    open fun onActivityCreated(navigator: Navigator, activity: Activity) {}
}