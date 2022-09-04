package com.therouter.router.action.interceptor

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.therouter.router.KEY_OBJECT_ACTIVITY
import com.therouter.router.Navigator
import com.therouter.router.arguments

abstract class ActionInterceptor {

    fun optActivity(): Activity? = arguments[KEY_OBJECT_ACTIVITY]?.get() as? Activity?

    abstract fun handle(context: Context, args: Bundle): Boolean

    fun onFinish() {}

    /**
     * 数字越大，优先级越高
     *
     * @return
     */
    open val priority: Int
        get() = 5
}