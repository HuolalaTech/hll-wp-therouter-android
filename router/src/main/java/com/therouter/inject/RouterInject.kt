package com.therouter.inject

import a.trojan
import android.content.Context
import androidx.annotation.Keep
import com.therouter.debug
import com.therouter.execute
import com.therouter.history.ServiceProviderHistory
import com.therouter.history.pushHistory
import dalvik.system.DexFile
import java.lang.reflect.Constructor
import java.util.*

class RouterInject {
    // 一般来说16个应该够了
    private val mInterceptors = TheRouterLinkedList<Interceptor>(16)
    private val mCustomInterceptors = LinkedList<Interceptor>()
    private val mRecyclerBin = RecyclerBin()

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
        mCustomInterceptors.addFirst(factory)
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
                temp = mRecyclerBin.put(clazz, temp, *params)
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
        //首先保证不会在读取的时候另一个线程不会对集合有增删操作
        mInterceptors.readLock().lock()
        for (f in mInterceptors) {
            t = f.interception(tClass, *params)
            if (t != null) {
                routerInjectDebugLog("interception::===" + tClass + "===" + t.javaClass.simpleName + t.hashCode())
                try {
                    mInterceptors.readLock().unlock()
                } catch (e: Exception) {
                }
                return t
            }
        }
        try {
            mInterceptors.readLock().unlock()
        } catch (e: Exception) {
        }

        //正常情况都会有创建器，如果没有，则使用默认创建器
        if (tClass.isInterface) {
            routerInjectDebugLog("$tClass is interface, but do not have @ServiceProvider")
        } else if (isNumberClass(tClass.name)) {
            routerInjectDebugLog("$tClass is primitive data types, but do not have @ServiceProvider")
        } else {
            val paramsClass = if (params.isNotEmpty()) {
                val temp = arrayOfNulls<Class<*>?>(params.size)
                for (i in params.indices) {
                    temp[i] = params[i]?.javaClass
                }
                temp
            } else arrayOfNulls<Class<*>?>(0)
            try {
                val constructor: Constructor<*> = tClass.getDeclaredConstructor(*paramsClass)
                if (!constructor.isAccessible) {
                    constructor.isAccessible = true
                }
                t = constructor.newInstance(*params) as T
            } catch (e: Exception) {
                routerInjectDebugLog(tClass.toString() + " do not have @ServiceProvider class. And constructor error::" + e.message) {
                    e.printStackTrace()
                }
            }
        }
        return t
    }

    private fun isNumberClass(type: String) = when (type) {
        "kotlin.Byte" -> true
        "kotlin.Short" -> true
        "kotlin.Int" -> true
        "kotlin.Long" -> true
        "kotlin.Float" -> true
        "kotlin.Double" -> true
        "kotlin.Boolean" -> true
        "kotlin.Char" -> true
        "java.lang.Byte" -> true
        "java.lang.Short" -> true
        "java.lang.Integer" -> true
        "java.lang.Long" -> true
        "java.lang.Float" -> true
        "java.lang.Double" -> true
        "java.lang.Boolean" -> true
        "java.lang.Character" -> true
        "java.lang.String" -> true
        else -> false
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
            val LOCK: Byte = 1
            val NONE: Byte = 0
            val UNLOCK: Byte = -1
            var isLock = NONE
            while (entries.hasMoreElements()) {
                val name = entries.nextElement() as String
                if (name.startsWith("$PACKAGE.$SUFFIX")) {
                    //保证不会被同时写入
                    if (isLock <= 0) {
                        mInterceptors.writeLock().lock()
                        isLock = LOCK
                    }
                    val clazz = Class.forName(name)
                    if (Interceptor::class.java.isAssignableFrom(clazz) && Interceptor::class.java != clazz) {
                        mInterceptors.add(clazz.newInstance() as Interceptor)
                    }
                } else {
                    if (isLock > 0) {
                        try {
                            mInterceptors.writeLock().unlock()
                        } catch (e: Exception) {
                        }
                        isLock = UNLOCK
                    }
                }
            }
        } catch (e: Exception) {
            routerInjectDebugLog("getAllDI error") {
                e.printStackTrace()
            }
        } finally {
            // 防止迭代器最后一次锁上后没有解锁的情况
            try {
                mInterceptors.writeLock().unlock()
            } catch (e: Exception) {
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