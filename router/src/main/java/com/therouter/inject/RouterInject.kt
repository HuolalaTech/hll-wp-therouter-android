package com.therouter.inject

import a.trojan
import android.content.Context
import androidx.annotation.Keep
import com.therouter.debug
import com.therouter.execute
import com.therouter.history.ServiceProviderHistory
import com.therouter.history.pushHistory
import java.util.concurrent.CopyOnWriteArrayList

private val mRecyclerBin = RecyclerBin()

class RouterInject {
    // 一般来说16个应该够了
    private val mInterceptors = CopyOnWriteArrayList<Interceptor>()
    private val mCustomInterceptors = CopyOnWriteArrayList<Interceptor>()

    fun asyncInitRouterInject(context: Context?) = execute {
        syncInitRouterInject(context)
    }

    fun syncInitRouterInject(context: Context?) {
        trojan()
        if (mInterceptors.isEmpty()) {
            initServiceProvider(context)
        }
    }

    internal fun initServiceProvider(context: Context?) {
        getAllDI(context)
        mInterceptors.addAll(getServiceProviderIndex())
    }

    @Keep
    fun addInterceptor(factory: Interceptor) {
        mCustomInterceptors.add(factory)
    }

    /**
     * library内部方法，禁止调用
     */
    @Keep
    fun privateAddInterceptor(factory: Interceptor) {
        mInterceptors.add(factory)
    }

    operator fun <T> get(clazz: Class<T>, vararg params: Any?): T? {
        var strArgs = ""
        params.forEach { strArgs += "$it, " }
        pushHistory(ServiceProviderHistory("${clazz}.provider(${strArgs})"))
        var temp = mRecyclerBin.get(clazz, *params)
        if (temp == null) {
            temp = createDI(clazz, *params)
            if (temp != null) {
                mRecyclerBin.put(clazz, temp, *params)
            }
        }
        return temp
    }

    private fun <T> createDI(tClass: Class<T>, vararg params: Any?): T? {
        var t: T? = null
        //查找自定义拦截器
        for (f in mCustomInterceptors) {
            t = f.interception(tClass, *params)
            if (t != null) {
                routerInjectDebugLog("mCustomInterceptors::===" + tClass + "===" + t.javaClass.simpleName + t.hashCode())
                return t
            }
        }
        //查找 ServiceProvider
        for (f in mInterceptors) {
            t = f.interception(tClass, *params)
            if (t != null) {
                routerInjectDebugLog("interception::===" + tClass + "===" + t.javaClass.simpleName + t.hashCode())
                return t
            }
        }
        return t
    }

}

private fun routerInjectDebugLog(msg: String, block: () -> Unit = {}) {
    debug("RouterInject", msg, block)
}