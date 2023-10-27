package com.therouter

import a.*
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.therouter.TheRouter.logCat
import com.therouter.flow.Digraph
import com.therouter.flow.runInitFlowTask
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
 * TheRouter 是否初始化完成
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
     * 自定义log输出方式，例如可将某些指定Tag上报到服务端，仅Release包有效，Debug包永远输出在控制台
     */
    @JvmStatic
    var logCat: (tag: String, msg: String) -> Unit? = { _, _ -> }

    val digraph = Digraph()

    /**
     * TheRouter初始化方法。内部流程：<br>
     * 同步流程：<br>
     *     1. 首先初始化FlowTask的内置事件，BEFORE_THEROUTER_INITIALIZATION，以及依赖这个Task的全部任务。
     *         这个事件的目的是在TheRouter的路由初始化前做某些操作，例如修改路由表、添加路由拦截器等……
     *     2. 初始化跨模块依赖表
     *     3. 初始化路由表
     * 异步流程：<br>
     *     1. 调用FlowTask的外部事件
     *     2. 添加 @Autowired 路由解析器
     */
    @JvmStatic
    fun init(context: Context?) {
        init(context, true)
    }

    @JvmStatic
    fun init(context: Context?, asyncInitRouterInject: Boolean) {
        if (!inited) {
            debug("init", "TheRouter init start!")
            addFlowTask(context, digraph)
            debug("init", "TheRouter.init() method do @FlowTask before task")
            digraph.beforeSchedule()
            execute {
                debug("init", "TheRouter.init() method do @FlowTask init")
                digraph.initSchedule()
                debug("init", "TheRouter.init() method do @FlowTask schedule")
                runInitFlowTask()
            }
            if (asyncInitRouterInject) {
                routerInject.asyncInitRouterInject(context)
            } else {
                routerInject.syncInitRouterInject(context)
            }
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
    fun addActionInterceptor(action: String?, interceptor: ActionInterceptor?) =
        ActionManager.addActionInterceptor(action, interceptor)

    /**
     * 移除所有action对应的拦截器，如果action有多个拦截器，则都会被移除
     */
    @JvmStatic
    fun removeAllInterceptorForKey(action: String?) =
        ActionManager.removeAllInterceptorForKey(action)

    /**
     * 移除所有指定拦截器，如果有多个action共用同一个拦截器，则都会被移除
     */
    @JvmStatic
    fun removeAllInterceptorForValue(interceptor: ActionInterceptor?) =
        ActionManager.removeAllInterceptorForValue(interceptor)

    /**
     * 删除 Action 拦截器
     */
    @JvmStatic
    fun removeActionInterceptor(action: String?, interceptor: ActionInterceptor?) =
        ActionManager.removeActionInterceptor(action, interceptor)

    /**
     * 执行业务自定义的 FlowTask，仅支持自定义业务节点的 Task
     */
    @JvmStatic
    fun runTask(taskName: String) {
        if (digraph.inited) {
            digraph.getVirtualTask(taskName).run()
        } else {
            digraph.addPendingRunnable {
                digraph.getVirtualTask(taskName).run()
            }
        }
    }

    /**
     * 判断 url 是否为 TheRouter 的 Action
     * Path 会被记录到路由表内，Action不会被记录，Action更像是一个消息事件，参考Android的广播
     */
    @JvmStatic
    fun isRouterAction(url: String?) = ActionManager.isAction(build(url))

    /**
     * 判断 url 是否为 TheRouter 的路由 Path
     * Path 会被记录到路由表内，Action不会被记录，Action更像是一个消息事件，参考Android的广播
     */
    @JvmStatic
    fun isRouterPath(url: String?) = matchRouteMap(url) != null

    /**
     * 获取跨模块依赖的服务
     */
    @JvmStatic
    fun <T> get(clazz: Class<T>, vararg params: Any?): T? {
        return routerInject.get(clazz, *params)
    }

    /**
     * 通过Path构建路由导航器
     */
    @JvmStatic
    fun build(url: String?): Navigator {
        return Navigator(url)
    }

    /**
     * 通过Intent构建路由导航器
     */
    @JvmStatic
    fun build(it: Intent): Navigator {
        return Navigator(foundPathFromIntent(it), it)
    }

    /**
     * 为 @Autowired 注解的变量赋值
     */
    @JvmStatic
    fun inject(any: Any?) {
        autowiredInject(any)
    }
}

/**
 * 打印日志，允许通过 TheRouter.logCat 自定义日志输出
 */
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
