package com.therouter.router

import android.app.Activity
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import androidx.fragment.app.Fragment
import com.therouter.TheRouter
import com.therouter.TheRouterLifecycleCallback
import com.therouter.debug
import com.therouter.getApplicationContext
import com.therouter.history.ActivityNavigatorHistory
import com.therouter.history.FragmentNavigatorHistory
import com.therouter.history.pushHistory
import com.therouter.router.action.ActionManager
import com.therouter.router.interceptor.*
import java.io.Serializable
import java.lang.ref.SoftReference
import com.therouter.require
import java.util.*

private val disposableQueue = LinkedList<PendingNavigator>()
internal val arguments = HashMap<String, SoftReference<Any>>()

/**
 * 路由导航器。与RouterItem作用类似，允许互转。
 * RouterItem 用于描述一个静态的路由项
 * Navigator 用于描述一个路由项的跳转动作
 */
open class Navigator(var url: String?, val intent: Intent?) {
    val originalUrl = url
    val extras = Bundle()
    private var optionsCompat: Bundle? = null
    private var pending = false
    private var intentIdentifier: String? = null
    private var intentData: Uri? = null
    private var intentClipData: ClipData? = null

    val simpleUrl: String
        get() {
            val tempUrl = url ?: ""
            return if (tempUrl.contains("?")) {
                tempUrl.substring(0, tempUrl.indexOf('?'))
            } else tempUrl
        }

    constructor(url: String?) : this(url, null)

    init {
        require(!TextUtils.isEmpty(url), "Navigator", "Navigator constructor parameter url is empty")
        for (handle in fixHandles) {
            handle?.let {
                if (it.watch(url)) {
                    url = it.fix(url)
                }
            }
        }
//        uri = Uri.parse(url ?: "")
//        for (key in uri.queryParameterNames) {
//            // 通过url取到的value，都认为是string，autowired解析的时候会做兼容
//            extras.putString(key, uri.getQueryParameter(key))
//        }
        // queryParameterNames() 会自动decode，造成外部逻辑错误，所以这里需要根据&手动截取k=v
        // encodedQuery() 无法解析带#的url，例如 https://kymjs.com/#/index?k=v，会造成k=v丢失
        url?.let { noNullUrl ->
            val index = noNullUrl.indexOf('?')
            if (index >= 0 && noNullUrl.length > index) {
                noNullUrl.substring(index + 1)
            } else {
                noNullUrl
            }.split("&").forEach {
                val idx = it.indexOf("=")
                val key = if (idx > 0) it.substring(0, idx) else it
                val value: String? = if (idx > 0 && it.length > idx + 1) it.substring(idx + 1) else null
                // 通过url取到的value，都认为是string，autowired解析的时候会做兼容
                extras.putString(key, value)
            }
        }
    }

    fun getUrlWithParams() = getUrlWithParams { k, v -> "$k=$v" }

    fun getUrlWithParams(handle: NavigatorParamsFixHandle) = getUrlWithParams(handle::fix)

    fun getUrlWithParams(handle: (String, String) -> String): String {
        val stringBuilder = StringBuilder(simpleUrl)
        var isFirst = true
        for (key in extras.keySet()) {
            if (isFirst) {
                stringBuilder.append("?")
                isFirst = false
            } else {
                stringBuilder.append("&")
            }
            stringBuilder.append(handle(key, extras.get(key)?.toString() ?: ""))
        }
        return stringBuilder.toString()
    }

    fun pending(): Navigator {
        pending = true
        return this
    }

    fun withInt(key: String?, value: Int): Navigator {
        extras.putInt(key, value)
        return this
    }

    fun withLong(key: String?, value: Long): Navigator {
        extras.putLong(key, value)
        return this
    }

    fun withDouble(key: String?, value: Double): Navigator {
        extras.putDouble(key, value)
        return this
    }

    fun withFloat(key: String?, value: Float): Navigator {
        extras.putFloat(key, value)
        return this
    }

    fun withChar(key: String?, value: Char): Navigator {
        extras.putChar(key, value)
        return this
    }

    fun withByte(key: String?, value: Byte): Navigator {
        extras.putByte(key, value)
        return this
    }

    fun withBoolean(key: String?, value: Boolean): Navigator {
        extras.putBoolean(key, value)
        return this
    }

    fun withString(key: String?, value: String?): Navigator {
        extras.putString(key, value)
        return this
    }

    fun withSerializable(key: String?, value: Serializable?): Navigator {
        extras.putSerializable(key, value)
        return this
    }

    fun withParcelable(key: String?, value: Parcelable?): Navigator {
        extras.putParcelable(key, value)
        return this
    }

    fun withObject(key: String, value: Any): Navigator {
        arguments[key] = SoftReference(value)
        return this
    }

    fun with(value: Bundle?): Navigator = withBundle(KEY_BUNDLE, value)

    fun fillParams(action: (Bundle) -> Unit): Navigator {
        action(extras)
        return this
    }

    fun withBundle(key: String?, value: Bundle?): Navigator {
        extras.putBundle(key, value)
        return this
    }

    fun addFlags(flags: Int): Navigator {
        extras.putInt(KEY_INTENT_FLAGS, extras.getInt(KEY_INTENT_FLAGS, 0) or flags)
        return this
    }

    fun withFlags(flags: Int): Navigator {
        extras.putInt(KEY_INTENT_FLAGS, flags)
        return this
    }

    fun withOptionsCompat(options: Bundle?): Navigator {
        this.optionsCompat = options
        return this
    }

    fun withInAnimation(id: Int): Navigator {
        extras.putInt(KEY_ANIM_IN, id)
        return this
    }

    fun withOutAnimation(id: Int): Navigator {
        extras.putInt(KEY_ANIM_OUT, id)
        return this
    }

    fun setData(uri: Uri?): Navigator {
        intentData = uri
        return this
    }

    fun setIdentifier(identifier: String?): Navigator {
        intentIdentifier = identifier
        return this
    }

    fun setClipData(clipData: ClipData?): Navigator {
        intentClipData = clipData
        return this
    }

    fun optObject(key: String) = arguments[key]?.get()

    /**
     * 通过导航器创建Intent，会自动将Navigator中的参数传入Intent，异步回调返回
     * intent.putExtra(KEY_ACTION, routeItem.action)
     * intent.putExtra(KEY_PATH, getUrlWithParams())
     * intent.putExtra(KEY_DESCRIPTION, routeItem.description)
     */
    fun createIntentWithCallback(ctx: Context?, callback: (Intent) -> Unit) {
        if (!initedRouteMap || pending) {
            pending = true
            debug("Navigator::createIntentWithCallback", "add pending navigator $simpleUrl")
            disposableQueue.addLast(PendingNavigator(this) {
                pending = false
                callback(createIntent(ctx))
            })
        } else {
            callback(createIntent(ctx))
        }
    }

    /**
     * 通过导航器创建Intent，会自动将Navigator中的参数传入Intent
     * intent.putExtra(KEY_ACTION, routeItem.action)
     * intent.putExtra(KEY_PATH, getUrlWithParams())
     * intent.putExtra(KEY_DESCRIPTION, routeItem.description)
     */
    fun createIntent(ctx: Context?): Intent {
        debug("Navigator::createIntent", "begin navigate $simpleUrl")
        val context = ctx ?: getApplicationContext()
        var matchUrl: String? = simpleUrl
        for (interceptor in pathReplaceInterceptors) {
            interceptor?.let {
                if (it.watch(matchUrl)) {
                    val temp = matchUrl
                    matchUrl = it.replace(matchUrl)
                    debug("Navigator::createIntent", "$temp replace to $matchUrl")
                }
            }
        }
        var match = matchRouteMap(matchUrl)
        match?.getExtras()?.putAll(extras)
        match?.let {
            debug("Navigator::createIntent", "match route $it")
        }
        for (interceptor in routerReplaceInterceptors) {
            interceptor?.let {
                if (it.watch(match)) {
                    match = it.replace(match)
                    match?.let { routeItem ->
                        debug("Navigator::createIntent", "route replace to $routeItem")
                    }
                }
            }
        }
        val navigationIntent = intent ?: Intent()
        if (match != null) {
            routerInterceptor.invoke(match!!) { routeItem ->
                intentData?.let {
                    navigationIntent.data = it
                }
                intentClipData?.let {
                    navigationIntent.clipData = it
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && intentIdentifier != null) {
                    navigationIntent.identifier = intentIdentifier
                }
                navigationIntent.component = ComponentName(context!!.packageName, routeItem.className)
                if (context !is Activity) {
                    navigationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                TheRouterLifecycleCallback.addActivityCreatedObserver(routeItem.className) {
                    if (it.javaClass.name == routeItem.className) {
                        if (!TextUtils.isEmpty(routeItem.action)) {
                            TheRouter.build(routeItem.action)
                                .withObject(KEY_OBJECT_NAVIGATOR, this)
                                .withObject(KEY_OBJECT_ACTIVITY, it)
                                .action(it)
                        }
                    }
                }
                navigationIntent.putExtra(KEY_ACTION, routeItem.action)
                navigationIntent.putExtra(KEY_PATH, getUrlWithParams())
                navigationIntent.putExtra(KEY_DESCRIPTION, routeItem.description)
                with(routeItem.getExtras()) {
                    val bundle: Bundle? = getBundle(KEY_BUNDLE)
                    if (bundle != null) {
                        remove(KEY_BUNDLE)
                        navigationIntent.putExtra(KEY_BUNDLE, bundle)
                    }
                    navigationIntent.putExtras(this)
                }
                navigationIntent.addFlags(routeItem.getExtras().getInt(KEY_INTENT_FLAGS))
                val inAnimId = routeItem.getExtras().getInt(KEY_ANIM_IN)
                val outAnimId = routeItem.getExtras().getInt(KEY_ANIM_OUT)
                if (inAnimId != 0 || outAnimId != 0) {
                    if (context is Activity) {
                        debug("Navigator::createIntent", "overridePendingTransition ${routeItem.className}")
                        context.overridePendingTransition(
                            routeItem.getExtras().getInt(KEY_ANIM_IN),
                            routeItem.getExtras().getInt(KEY_ANIM_OUT)
                        )
                    } else {
                        if (TheRouter.isDebug) {
                            throw RuntimeException("Navigator::createIntent context is not Activity, ignore animation")
                        }
                    }
                }
            }
        }
        return navigationIntent
    }

    /**
     * 通过导航器创建Fragment，异步回调
     * 接收方可通过argus或@Autowired获取参数
     */
    fun <T : Fragment?> createFragmentWithCallback(callback: (T?) -> Unit) {
        if (!initedRouteMap || pending) {
            pending = true
            debug("Navigator::createFragmentWithCallback", "add pending navigator $simpleUrl")
            disposableQueue.addLast(PendingNavigator(this) {
                pending = false
                callback(createFragment() as T?)
            })
        } else {
            callback(createFragment() as T?)
        }
    }

    /**
     * 通过导航器创建Fragment，当TheRouter没有初始化完成时，将返回null
     * 接收方可通过argus或@Autowired获取参数
     */
    fun <T : Fragment?> createFragment(): T? {
        var fragment: Fragment? = null
        debug("Navigator::navigationFragment", "begin navigate $simpleUrl")
        var matchUrl: String? = simpleUrl
        for (interceptor in pathReplaceInterceptors) {
            interceptor?.let {
                if (it.watch(matchUrl)) {
                    matchUrl = it.replace(matchUrl)
                }
            }
        }
        debug("Navigator::navigationFragment", "path replace to $matchUrl")
        var match = matchRouteMap(matchUrl)
        match?.getExtras()?.putAll(extras)
        match?.let {
            debug("Navigator::navigationFragment", "match route $it")
        }
        for (interceptor in routerReplaceInterceptors) {
            interceptor?.let {
                if (it.watch(match)) {
                    match = it.replace(match)
                }
            }
        }
        debug("Navigator::navigationFragment", "route replace to $match")
        match?.let {
            routerInterceptor.invoke(match!!) { routeItem ->
                if (isFragmentClass(routeItem.className)) {
                    try {
                        fragment = instantiate(routeItem.className)
                        val bundle = routeItem.getExtras()
                        intent?.extras?.let { bundle.putAll(it) }
                        bundle.putString(KEY_ACTION, routeItem.action)
                        bundle.putString(KEY_PATH, getUrlWithParams())
                        bundle.putString(KEY_DESCRIPTION, routeItem.description)
                        fragment?.arguments = bundle
                        debug("Navigator::navigation", "create fragment ${routeItem.className}")
                    } catch (e: Exception) {
                        debug("Navigator::navigationFragment", "create fragment instance error") {
                            e.printStackTrace()
                        }
                    }
                    pushHistory(FragmentNavigatorHistory(getUrlWithParams()))
                } else {
                    if (TheRouter.isDebug) {
                        throw RuntimeException("TheRouter::Navigator ${routeItem.className} is not Fragment")
                    }
                }
            }
        }
        return fragment as T?
    }

    @JvmOverloads
    fun navigation(fragment: Fragment?, callback: NavigationCallback? = null) {
        navigation(fragment, DEFAULT_REQUEST_CODE, callback)
    }

    @JvmOverloads
    fun navigation(fragment: Fragment?, requestCode: Int, ncb: NavigationCallback? = null) {
        navigation(fragment?.activity, fragment, requestCode, ncb)
    }

    @JvmOverloads
    fun navigation(context: Context? = getApplicationContext(), callback: NavigationCallback? = null) {
        navigation(context, DEFAULT_REQUEST_CODE, callback)
    }

    @JvmOverloads
    fun navigation(ctx: Context?, requestCode: Int, ncb: NavigationCallback? = null) {
        navigation(ctx, null, requestCode, ncb)
    }

    /**
     * 跳转到对应Activity落地页
     */
    @JvmOverloads
    fun navigation(ctx: Context?, fragment: Fragment?, requestCode: Int, ncb: NavigationCallback? = null) {
        if (!initedRouteMap || pending) {
            pending = true
            debug("Navigator::navigation", "add pending navigator $simpleUrl")
            disposableQueue.addLast(PendingNavigator(this) {
                pending = false
                this.navigation(ctx, fragment, requestCode, ncb)
            })
            return
        }
        debug("Navigator::navigation", "begin navigate $simpleUrl")
        val context = ctx ?: getApplicationContext()
        val callback = ncb ?: defaultCallback
        var matchUrl: String? = simpleUrl
        for (interceptor in pathReplaceInterceptors) {
            interceptor?.let {
                if (it.watch(matchUrl)) {
                    val temp = matchUrl
                    matchUrl = it.replace(matchUrl)
                    debug("Navigator::navigation", "$temp replace to $matchUrl")
                }
            }
        }
        var match = matchRouteMap(matchUrl)

        // navigator can not jump, but ActionManager can handle it.
        if (ActionManager.isAction(this) && match == null) {
            ActionManager.handleAction(this, context)
            return
        }

        match?.getExtras()?.putAll(extras)
        match?.let {
            debug("Navigator::navigation", "match route $it")
        }
        for (interceptor in routerReplaceInterceptors) {
            interceptor?.let {
                if (it.watch(match)) {
                    match = it.replace(match)
                    match?.let { routeItem ->
                        debug("Navigator::navigation", "route replace to $routeItem")
                    }
                }
            }
        }
        if (match != null) {
            debug("Navigator::navigation", "NavigationCallback on found")
            callback.onFound(this)
            routerInterceptor.invoke(match!!) { routeItem ->
                val intent = intent ?: Intent()
                intentData?.let {
                    intent.data = it
                }
                intentClipData?.let {
                    intent.clipData = it
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && intentIdentifier != null) {
                    intent.identifier = intentIdentifier
                }
                intent.component = ComponentName(context!!.packageName, routeItem.className)
                if (context !is Activity && fragment == null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                TheRouterLifecycleCallback.addActivityCreatedObserver(routeItem.className) {
                    if (it.javaClass.name == routeItem.className) {
                        callback.onActivityCreated(this, it)
                        if (!TextUtils.isEmpty(routeItem.action)) {
                            TheRouter.build(routeItem.action)
                                .withObject(KEY_OBJECT_NAVIGATOR, this)
                                .withObject(KEY_OBJECT_ACTIVITY, it)
                                .action(it)
                        }
                    }
                }
                intent.putExtra(KEY_ACTION, routeItem.action)
                intent.putExtra(KEY_PATH, getUrlWithParams())
                intent.putExtra(KEY_DESCRIPTION, routeItem.description)
                with(routeItem.getExtras()) {
                    val bundle: Bundle? = getBundle(KEY_BUNDLE)
                    if (bundle != null) {
                        remove(KEY_BUNDLE)
                        intent.putExtra(KEY_BUNDLE, bundle)
                    }
                    intent.putExtras(this)
                }
                intent.addFlags(routeItem.getExtras().getInt(KEY_INTENT_FLAGS))
                if (requestCode == DEFAULT_REQUEST_CODE) {
                    if (fragment != null) {
                        debug("Navigator::navigation", "fragment.startActivity ${routeItem.className}")
                        fragment.startActivity(intent, optionsCompat)
                    } else {
                        debug("Navigator::navigation", "startActivity ${routeItem.className}")
                        context.startActivity(intent, optionsCompat)
                    }
                    val inAnimId = routeItem.getExtras().getInt(KEY_ANIM_IN)
                    val outAnimId = routeItem.getExtras().getInt(KEY_ANIM_OUT)
                    if (inAnimId != 0 || outAnimId != 0) {
                        if (context is Activity) {
                            debug("Navigator::navigation", "overridePendingTransition ${routeItem.className}")
                            context.overridePendingTransition(
                                routeItem.getExtras().getInt(KEY_ANIM_IN),
                                routeItem.getExtras().getInt(KEY_ANIM_OUT)
                            )
                        } else {
                            if (TheRouter.isDebug) {
                                throw RuntimeException("TheRouter::Navigator context is not Activity, ignore animation")
                            }
                        }
                    }
                } else {
                    if (fragment != null) {
                        debug("Navigator::navigation", "fragment.startActivityForResult ${routeItem.className}")
                        fragment.startActivityForResult(intent, requestCode, optionsCompat)
                    } else if (context is Activity) {
                        debug("Navigator::navigation", "startActivityForResult ${routeItem.className}")
                        context.startActivityForResult(intent, requestCode, optionsCompat)
                    } else {
                        if (TheRouter.isDebug) {
                            throw RuntimeException("TheRouter::Navigator context is not Activity or Fragment")
                        } else {
                            context.startActivity(intent, optionsCompat)
                        }
                    }
                }
                pushHistory(ActivityNavigatorHistory(getUrlWithParams()))
            }
            callback.onArrival(this)
        } else {
            callback.onLost(this)
        }
    }

    /**
     * 执行导航器Action
     */
    fun action() {
        action(null)
    }

    /**
     * 执行导航器Action
     */
    fun action(ctx: Context? = null) {
        if (ActionManager.isAction(this)) {
            navigation(ctx)
        }
    }
}

const val KEY_ACTION = "therouter_action"
const val KEY_PATH = "therouter_path"
const val KEY_DESCRIPTION = "therouter_description"
const val KEY_BUNDLE = "therouter_bundle"
const val KEY_INTENT_FLAGS = "therouter_intent_flags"
const val KEY_ANIM_IN = "therouter_intent_animation_in"
const val KEY_ANIM_OUT = "therouter_intent_animation_out"
const val KEY_OBJECT_NAVIGATOR = "therouter_object_navigator"
const val KEY_OBJECT_ACTIVITY = "therouter_object_current_activity"
internal const val DEFAULT_REQUEST_CODE = -1008600
private val fixHandles: MutableList<NavigatorPathFixHandle?> = ArrayList()
private val pathReplaceInterceptors: MutableList<PathReplaceInterceptor?> = ArrayList()
private val routerReplaceInterceptors: MutableList<RouterReplaceInterceptor?> = ArrayList()
private var defaultCallback: NavigationCallback = NavigationCallback()
private var routerInterceptor = { route: RouteItem, callback: (RouteItem) -> Unit ->
    callback.invoke(route)
}

/**
 * 自定义全局路由跳转结果回调
 *
 * 如果使用TheRouter跳转，传入了一个不识别的的path，则不会有任何处理。你也可以定义一个默认的全局回调，来处理跳转情况，如果落地页是 Fragment 则不会回调。
 * 当然，跳转结果的回调不止这一个用途，可以根据业务有自己的处理。
 * 回调也可以单独为某一次跳转设置，navigation()方法有重载可以传入设置。
 */
fun defaultNavigationCallback(callback: NavigationCallback?) {
    callback?.let {
        defaultCallback = it
    }
}

/**
 * 应用场景：用于修复客户端上路由 path 错误问题。
 * 例如：相对路径转绝对路径，或由于服务端下发的链接无法固定https或http，但客户端代码写死了 https 的 path，就可以用这种方式统一。
 * 注：必须在 TheRouter.build() 方法调用前添加处理器，否则处理器前的所有path不会被修改。
 */
fun addNavigatorPathFixHandle(handle: NavigatorPathFixHandle) {
    fixHandles.add(handle)
    Collections.sort(fixHandles, Comparator { o1, o2 ->
        return@Comparator if (o1 == null) {
            -1
        } else if (o2 == null) {
            1
        } else o2.priority - o1.priority
    })
}

/**
 * 移除Path修改器
 */
fun removeNavigatorPathFixHandle(interceptor: NavigatorPathFixHandle): Boolean {
    return fixHandles.remove(interceptor)
}

/**
 * 页面替换器
 * 应用场景：需要将某些path指定为新链接的时候使用。 也可以用在修复链接的场景，但是与 path 修改器不同的是，修改器通常是为了解决通用性的问题，替换器只在页面跳转时才会生效，更多是用来解决特性问题。
 *
 * 例如模块化的时候，首页壳模板组件中开发了一个SplashActivity广告组件作为应用的MainActivity，在闪屏广告结束的时候自动跳转业务首页页面。 但是每个业务不同，首页页面的 Path 也不相同，而不希望让每个业务线自己去改这个首页壳模板组件，此时就可以组件中先写占位符https://kymjs.com/splash/to/home，让接入方通过 Path 替换器解决。
 * 注：必须在 TheRouter.build().navigation() 方法调用前添加处理器，否则处理器前的所有跳转不会被替换。
 */
fun addPathReplaceInterceptor(interceptor: PathReplaceInterceptor) {
    pathReplaceInterceptors.add(interceptor)
    Collections.sort(pathReplaceInterceptors, Comparator { o1, o2 ->
        return@Comparator if (o1 == null) {
            -1
        } else if (o2 == null) {
            1
        } else o2.priority - o1.priority
    })
}

fun removePathReplaceInterceptor(interceptor: PathReplaceInterceptor): Boolean {
    return pathReplaceInterceptors.remove(interceptor)
}

/**
 * 路由替换器
 * 应用场景：常用在未登录不能使用的页面上。例如访问用户钱包页面，在钱包页声明的时候，可以在路由表上声明本页面是需要登录的，在路由跳转过程中，如果落地页是需要登录的，则先替换路由到登录页，同时将原落地页信息作为参数传给登录页，登录流程处理完成后可以继续执行之前的路由操作。
 *
 * 路由替换器的拦截点更靠后，主要用于框架已经从路由表中根据 path 找到路由以后，对找到的路由做操作。
 *
 * 这种逻辑在所有页面跳转前写不太合适，以前的做法通常是在落地页写逻辑判断用户是否具有权限，但其实在路由层完成更合适。
 * 注：必须在 TheRouter.build().navigation() 方法调用前添加处理器，否则处理器前的所有跳转不会被替换。
 */
fun addRouterReplaceInterceptor(interceptor: RouterReplaceInterceptor) {
    routerReplaceInterceptors.add(interceptor)
    Collections.sort(routerReplaceInterceptors, Comparator { o1, o2 ->
        return@Comparator if (o1 == null) {
            -1
        } else if (o2 == null) {
            1
        } else o2.priority - o1.priority
    })
}

fun removeRouterReplaceInterceptor(interceptor: RouterReplaceInterceptor): Boolean {
    return routerReplaceInterceptors.remove(interceptor)
}

/**
 * 路由AOP拦截器
 * 与前三个处理器不同的点在于，路由的AOP拦截器全局只能有一个。用于实现AOP的能力，在整个TheRouter跳转的过程中，跳转前，目标页是否找到的回调，跳转时，跳转后，都可以做一些自定义的逻辑处理。
 *
 * 使用场景：场景很多，最常用的是可以拦截一些跳转，例如debug页面在生产环境不打开，或定制startActivity跳转方法。
 */
fun setRouterInterceptor(interceptor: RouterInterceptor) {
    routerInterceptor = { route: RouteItem, callback: (RouteItem) -> Unit ->
        interceptor.process(route, object : InterceptorCallback {
            override fun onContinue(routeItem: RouteItem) {
                callback.invoke(routeItem)
            }
        })
    }
}

/**
 * 路由AOP拦截器
 * 与前三个处理器不同的点在于，路由的AOP拦截器全局只能有一个。用于实现AOP的能力，在整个TheRouter跳转的过程中，跳转前，目标页是否找到的回调，跳转时，跳转后，都可以做一些自定义的逻辑处理。
 *
 * 使用场景：场景很多，最常用的是可以拦截一些跳转，例如debug页面在生产环境不打开，或定制startActivity跳转方法。
 */
fun setRouterInterceptor(interceptor: (routeItem: RouteItem, callback: (RouteItem) -> Unit) -> Unit) {
    routerInterceptor = interceptor
}

/**
 * 可以被挂起的导航器，通常用于延迟动作。
 * 延迟跳转主要应用场景有两种：
 * 第一种：初始化时期，如果路由表的量非常巨大时。这种情况在别的路由框架上要么会白屏一段时间，要么直接丢弃这次跳转。在TheRouter中，框架会暂存当前的跳转动作，在路由表初始化完成后立刻执行跳转。
 * 第二种：从Android 8.0开始，Activity 不能在后台启动页面，这对于业务判断造成了很大的影响。由于可能会有前台 Service 的情况，不能单纯以 Activity 生命周期判断前后台。在TheRouter中，框架允许业务自定义前后台规则，如果为后台情况，可以将跳转动作暂存，当进入前台后再恢复跳转。
 *
 * 回复跳转时需要调用
 * sendPendingNavigator(); //toplevel方法，无需类名调用，Java请通过NavigatorKt类名调用
 */
fun sendPendingNavigator() {
    disposableQueue.forEach { it.action() }
    disposableQueue.clear()
}