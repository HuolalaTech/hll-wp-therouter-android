package com.therouter.inject

import android.content.Context
import com.therouter.debug
import com.therouter.router.IRouterMapAPT
import dalvik.system.DexFile

const val PACKAGE = "a"
const val PREFIX_SERVICE_PROVIDER = "ServiceProvider__TheRouter__"
const val PREFIX_ROUTER_MAP = "RouterMap__TheRouter__"
const val SUFFIX_AUTOWIRED_DOT_CLASS = "__TheRouter__Autowired.class"
const val SUFFIX_AUTOWIRED = "__TheRouter__Autowired"

private val serviceProviderIndex = ArrayList<Interceptor>()
private val routerMapIndex = ArrayList<IRouterMapAPT>()

// 0: init progress, 1: init finish, -1: not init
@Volatile
private var inited = -1

/**
 * 将所有的 Interceptor 生成对象传入集合中
 * 兼容性方案，正常情况下不会被执行
 *
 * @WorkerThread
 */
internal fun getAllDI(context: Context?) {
    if (context == null || inited >= 0) {
        return
    }
    inited = 0
    try {
        val info = context.packageManager.getApplicationInfo(context.packageName, 0)
        val path = info.sourceDir
        val dexfile = DexFile(path)
        val entries = dexfile.entries()
        while (entries.hasMoreElements()) {
            val name = entries.nextElement()
            if (name.startsWith("$PACKAGE.$PREFIX_SERVICE_PROVIDER")) {
                val clazz = Class.forName(name)
                if (Interceptor::class.java.isAssignableFrom(clazz) && Interceptor::class.java != clazz) {
                    serviceProviderIndex.add(clazz.newInstance() as Interceptor)
                }
            } else if (name.startsWith("$PACKAGE.$PREFIX_ROUTER_MAP")) {
                val clazz = Class.forName(name)
                if (IRouterMapAPT::class.java.isAssignableFrom(clazz) && IRouterMapAPT::class.java != clazz) {
                    routerMapIndex.add(clazz.newInstance() as IRouterMapAPT)
                }
            }
        }
        inited = 1
    } catch (e: Exception) {
        debug("RouterInject", "getAllDI error") {
            e.printStackTrace()
        }
    }
}

internal fun getServiceProviderIndex() = serviceProviderIndex
internal fun getRouterMapIndex() = routerMapIndex

