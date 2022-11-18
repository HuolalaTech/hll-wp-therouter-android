package com.therouter

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.therouter.flow.splashInit

internal object TheRouterLifecycleCallback : ActivityLifecycleCallbacks {
    private var observer: ((Activity) -> Unit)? = {}

    private var appInited = false
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (!appInited) {
            appInited = true
            splashInit()
        }
        observer?.invoke(activity)
    }

    fun setActivityCreatedObserver(o: ((Activity) -> Unit)?) {
        observer = o
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}