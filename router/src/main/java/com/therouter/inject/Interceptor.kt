package com.therouter.inject

import android.content.Context
import com.therouter.flow.Digraph

/**
 * Created by ZhangTao on 17/8/1.
 */
interface Interceptor {
    fun <T> interception(clazz: Class<T>?, vararg params: Any?): T?
    fun initFlowTask(context: Context, digraph: Digraph)
}