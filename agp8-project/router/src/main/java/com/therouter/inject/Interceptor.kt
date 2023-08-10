package com.therouter.inject

/**
 * Created by ZhangTao on 17/8/1.
 */
interface Interceptor {
    fun <T> interception(clazz: Class<T>?, vararg params: Any?): T?
}