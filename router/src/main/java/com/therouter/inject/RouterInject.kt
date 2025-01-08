package com.therouter.inject

import a.trojan
import android.content.Context
import androidx.annotation.Keep
import com.therouter.debug
import com.therouter.execute
import com.therouter.history.ServiceProviderHistory
import com.therouter.history.pushHistory
import dalvik.system.DexFile
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

private val mRecyclerBin = RecyclerBin()

class RouterInject {
    // 一般来说16个应该够了
    private val mInterceptors = CopyOnWriteArrayList<Interceptor>()
    private val mCustomInterceptors = CopyOnWriteArrayList<Interceptor>()

    fun asyncInitRouterInject(context: Context?) {
        execute {
            trojan()
            if (mInterceptors.isEmpty()) {
                initServiceProvider(context)
            }
        }
    }

    fun syncInitRouterInject(context: Context?) {
        trojan()
        if (mInterceptors.isEmpty()) {
            initServiceProvider(context)
        }
    }

    internal fun initServiceProvider(context: Context?) {
        execute {
            getAllDI(context)
        }
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

    /**
     * 将所有的 Interceptor 生成对象传入集合中
     * 兼容性方案，性能较差，正常情况下不会被执行
     *
     * @WorkerThread
     */
    @Deprecated("")
    private fun getAllDI(context: Context?) {
        if (context == null) {
            return
        }
        try {
            val info = context.packageManager.getApplicationInfo(context.packageName, 0)
            val path = info.sourceDir
            val dexfile = DexFile(path)
            val entries: Enumeration<*> = dexfile.entries()
            while (entries.hasMoreElements()) {
                val name = entries.nextElement() as String
                if (name.startsWith("$PACKAGE.$SUFFIX")) {
                    val clazz = Class.forName(name)
                    if (Interceptor::class.java.isAssignableFrom(clazz) && Interceptor::class.java != clazz) {
                        mInterceptors.add(clazz.newInstance() as Interceptor)
                    }
                }
            }
        } catch (e: Exception) {
            routerInjectDebugLog("getAllDI error") {
                e.printStackTrace()
            }
        }
    }
}

const val PACKAGE = "a"
const val SUFFIX = "ServiceProvider__TheRouter__"
const val CLASS_NAME = "TheRouterServiceProvideInjecter"
private fun routerInjectDebugLog(msg: String, block: () -> Unit = {}) {
    debug("RouterInject", msg, block)
}