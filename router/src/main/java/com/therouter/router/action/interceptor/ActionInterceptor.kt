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

    /**
     * 每个Action都有一个对应的Arguments，如果一个Action有多个ActionInterceptor处理，则所有ActionInterceptor会根据优先级链式处理。
     * ActionInterceptor处理链间，可以通过Arguments，向下传递参数
     */
    fun getArguments() = args

    /**
     * 由框架内部使用，外部不应该调用本方法
     * 每个独立ActionInterceptor处理完后，会由ActionManager将处理后的Arguments暂存，并通过本方法传递给下一个拦截器使用
     */
    internal fun setArguments(b: Bundle) {
        args = b
    }

    @CallSuper
    open fun handle(context: Context, navigator: Navigator): Boolean = handle(context, navigator.extras)

    abstract fun handle(context: Context, args: Bundle): Boolean

    /**
     * 当前Action的所有处理器均处理完成以后回调。
     * 此时调用getArguments()获取的结果，与handle()中调用getArguments()获取的内容，可能不相同。
     * 因为你不能保证处理链后面的拦截器，是否修改了Arguments
     */
    fun onFinish() {}

    /**
     * 数字越大，优先级越高
     *
     * @return
     */
    open val priority: Int
        get() = 5
}