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
import java.io.UnsupportedEncodingException
import java.lang.ref.SoftReference
import java.net.URLEncoder
import com.therouter.require
import java.util.*

private val disposableQueue = LinkedList<PendingNavigator>()
internal val arguments = HashMap<String, SoftReference<Any>>()

open class Navigator(var url: String?, val intent: Intent?) {
    private val uri: Uri
    val normalUrl = url
    val extras = Bundle()
    private var optionsCompat: Bundle? = null
    private var pending = false
    private var intentIdentifier: String? = null
    private var intentData: Uri? = null
    private var intentClipData: ClipData? = null

    val simpleUrl: String
        get() {
            val url = uri.toString()
            return if (url.contains("?")) {
                url.substring(0, url.indexOf('?'))
            } else url
        }

    constructor(url: String?) : this(url, null)

    init {
        require(!TextUtils.isEmpty(url), "Navigator", "Navigator constructor parameter url is empty")
        for (handle in fixHandles) {
            handle?.let {
                url = it.fix(url)
            }
        }
        uri = Uri.parse(url ?: "")
//        for (key in uri.queryParameterNames) {
//            // 通过url取到的value，都认为是string，autowired解析的时候会做兼容
//            extras.putString(key, uri.getQueryParameter(key))
//        }
        // queryParameterNames() 会自动decode，造成外部逻辑错误，所以这里需要根据&手动截取k=v
        uri.encodedQuery?.split("&")?.forEach {
            val idx = it.indexOf("=")
            val key = if (idx > 0) it.substring(0, idx) else it
            val value: String? = if (idx > 0 && it.length > idx + 1) it.substring(idx + 1) else null
            // 通过url取到的value，都认为是string，autowired解析的时候会做兼容
            extras.putString(key, value)
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

    fun createIntent(ctx: Context?): Intent {
        debug("Navigator::createIntent", "begin navigate $simpleUrl")
        val context = ctx ?: getApplicationContext()
        var matchUrl: String? = simpleUrl
        for (interceptor in pathReplaceInterceptors) {
            interceptor?.let {
                matchUrl = interceptor.replace(matchUrl)
                debug("Navigator::createIntent", "path replace to $matchUrl")
            }
        }
        var match = matchRouteMap(matchUrl)
        match?.getExtras()?.putAll(extras)
        match?.let {
            debug("Navigator::createIntent", "match route $it")
        }
        for (interceptor in routerReplaceInterceptors) {
            interceptor?.let {
                match = interceptor.replace(match)
                match?.let {
                    debug("Navigator::createIntent", "route replace to $it")
                }
            }
        }
        // reset callback
        TheRouterLifecycleCallback.setActivityCreatedObserver {}
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
                TheRouterLifecycleCallback.setActivityCreatedObserver {
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

    fun <T : Fragment?> createFragment(): T? {
        var fragment: Fragment? = null
        debug("Navigator::navigationFragment", "begin navigate $simpleUrl")
        var matchUrl: String? = simpleUrl
        for (interceptor in pathReplaceInterceptors) {
            interceptor?.let {
                matchUrl = interceptor.replace(matchUrl)
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
                match = interceptor.replace(match)
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

    @JvmOverloads
    fun navigation(ctx: Context?, fragment: Fragment?, requestCode: Int, ncb: NavigationCallback? = null) {
        if (!initedRouteMap || pending) {
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
                matchUrl = interceptor.replace(matchUrl)
                debug("Navigator::navigation", "path replace to $matchUrl")
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
                match = interceptor.replace(match)
                match?.let {
                    debug("Navigator::navigation", "route replace to $it")
                }
            }
        }
        // reset callback
        TheRouterLifecycleCallback.setActivityCreatedObserver {}
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
                TheRouterLifecycleCallback.setActivityCreatedObserver {
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

    fun action() {
        action(null)
    }

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

fun defaultNavigationCallback(callback: NavigationCallback?) {
    callback?.let {
        defaultCallback = it
    }
}

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

fun removeNavigatorPathFixHandle(interceptor: NavigatorPathFixHandle): Boolean {
    return fixHandles.remove(interceptor)
}

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

fun setRouterInterceptor(interceptor: RouterInterceptor) {
    routerInterceptor = { route: RouteItem, callback: (RouteItem) -> Unit ->
        interceptor.process(route, object : InterceptorCallback {
            override fun onContinue(routeItem: RouteItem) {
                callback.invoke(routeItem)
            }
        })
    }
}

fun setRouterInterceptor(interceptor: (routeItem: RouteItem, callback: (RouteItem) -> Unit) -> Unit) {
    routerInterceptor = interceptor
}

fun sendPendingNavigator() {
    disposableQueue.forEach { it.action() }
    disposableQueue.clear()
}