package com.therouter

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.therouter.flow.splashInit
import java.lang.ref.WeakReference

internal object TheRouterLifecycleCallback : ActivityLifecycleCallbacks {
    private var observerMap: HashMap<String, WeakReference<(Activity) -> Unit>> = HashMap()

    private var appInited = false
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (!appInited) {
            appInited = true
            splashInit()
        }
        observerMap[activity.javaClass.name]?.get()?.invoke(activity)
    }

    fun addActivityCreatedObserver(key: String, o: ((Activity) -> Unit)?) {
        observerMap[key] = WeakReference(o)
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        observerMap.remove(activity.javaClass.name)
    }
}