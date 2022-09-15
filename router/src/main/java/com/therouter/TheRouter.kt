package com.therouter

import a.*
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.therouter.TheRouter.logCat
import com.therouter.flow.Digraph
import com.therouter.flow.applicationCreate
import com.therouter.inject.RouterInject
import com.therouter.router.*
import com.therouter.router.action.ActionManager
import com.therouter.router.action.interceptor.ActionInterceptor
import com.therouter.router.autowired.DefaultIdParser
import com.therouter.router.autowired.DefaultObjectParser
import com.therouter.router.autowired.DefaultServiceParser
import com.therouter.router.autowired.DefaultUrlParser
import com.therouter.router.interceptor.AutowiredParser
import java.util.*


/**
 * Created by ZhangTao on 17/8/1.
 */
private var inited = false

/**
 * 是否初始化完成
 */
fun theRouterInited() = inited

/**
 * 是否由框架自动初始化，如为false，则需要手动调用 TheRouter.init(context)
 * 此设置需要在Application.onCreate()之前修改
 */
var theRouterUseAutoInit = true

object TheRouter {
    @JvmStatic
    var isDebug = false

    @JvmStatic
    val parserList = LinkedList<AutowiredParser>()

    /**
     * @AutoWired 参数注入器
     */
    @JvmStatic
    val routerInject = RouterInject()

    /**
     * 自定义log输出方式
     */
    @JvmStatic
    var logCat: (tag: String, msg: String) -> Unit? = { _, _ -> }

    val digraph = Digraph()

    @JvmStatic
    fun init(context: Context?) {
        if (!inited) {
            debug("init", "TheRouter init start!")
            addFlowTask(context, digraph)
            debug("init", "TheRouter.init() method do @FlowTask before task")
            digraph.beforeSchedule()
            execute {
                debug("init", "TheRouter.init() method do @FlowTask init")
                digraph.initSchedule()
                debug("init", "TheRouter.init() method do @FlowTask schedule")
                digraph.schedule()
                applicationCreate()
            }
            routerInject.asyncInitRouterInject(context)
            asyncInitRouteMap()
            execute {
                context?.apply {
                    (applicationContext as Application).registerActivityLifecycleCallbacks(TheRouterLifecycleCallback)
                }
                parserList.addFirst(DefaultObjectParser())
                parserList.addFirst(DefaultServiceParser())
                parserList.addFirst(DefaultUrlParser())
                parserList.addFirst(DefaultIdParser())
            }
            debug("init", "TheRouter init finish!")
            inited = true
        }
    }

    /**
     * 新增 @Autowired 注解解析器
     */
    @JvmStatic
    fun addAutowiredParser(parser: AutowiredParser) {
        parserList.addFirst(parser)
    }

    /**
     * 新增 Action 拦截器
     */
    @JvmStatic
    fun addActionInterceptor(action: String?, interceptor: ActionInterceptor?) {
        ActionManager.addActionInterceptor(action, interceptor)
    }

    /**
     * 删除 Action 拦截器
     */
    @JvmStatic
    fun removeActionInterceptor(action: String?, interceptor: ActionInterceptor?) {
        ActionManager.removeActionInterceptor(action, interceptor)
    }

    /**
     * 执行业务自定义的 FlowTask
     */
    @JvmStatic
    fun runTask(taskName: String) {
        if (digraph.inited) {
            digraph.getVirtualTask(taskName)?.run()
        } else {
            digraph.addPendingRunnable {
                digraph.getVirtualTask(taskName)?.run()
            }
        }
    }

    @JvmStatic
    fun isRouterAction(url: String?) = ActionManager.isAction(build(url))

    @JvmStatic
    fun isRouterPath(url: String?) = matchRouteMap(url) != null

    /*********************************** hack ARouter  */
    @JvmStatic
    fun <T> get(clazz: Class<T>, vararg params: Any?): T? {
        return routerInject.get(clazz, *params)
    }

    @JvmStatic
    fun build(url: String?): Navigator {
        return Navigator(url)
    }

    @JvmStatic
    fun build(it: Intent): Navigator {
        return Navigator(foundPathFromIntent(it), it)
    }

    @JvmStatic
    fun inject(any: Any?) {
        autowiredInject(any)
    }
}

internal fun debug(tag: String, msg: String, block: () -> Unit = {}) {
    if (TheRouter.isDebug) {
        Log.d("TheRouter::$tag", msg)
        block.invoke()
    } else {
        logCat.invoke("TheRouter::$tag", msg)
    }
}

internal fun require(pass: Boolean, tag: String, msg: String) {
    if (!pass) {
        if (TheRouter.isDebug) {
            throw IllegalArgumentException("TheRouter::$tag::$msg")
        } else {
            logCat.invoke("TheRouter::$tag", msg)
        }
    }
}
