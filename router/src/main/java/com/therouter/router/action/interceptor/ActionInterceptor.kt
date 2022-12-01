package com.therouter.router.action.interceptor

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.annotation.CallSuper
import com.therouter.router.KEY_OBJECT_ACTIVITY
import com.therouter.router.Navigator
import com.therouter.router.arguments

abstract class ActionInterceptor {

    private var args: Bundle = Bundle()

    fun optActivity(): Activity? = arguments[KEY_OBJECT_ACTIVITY]?.get() as? Activity?
    fun getArguments() = args

    fun setArguments(b: Bundle) {
        args = b
    }

    @CallSuper
    open fun handle(context: Context, navigator: Navigator): Boolean = handle(context, navigator.extras)

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