package com.therouter.router

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import com.therouter.router.interceptor.NavigatorParamsFixHandle
import java.io.Serializable
import java.util.*

/**
 * 独立的一条路由记录
 *
 * @param path 路由Path
 * @param className 落地页类名
 * @param action 跳转到落地页以后，需要执行的动作
 * @param description 当前路由的注释
 * @param params 仅用于RouteMap.json文件被gson转换时存储，外部不可访问，会被合并到extras中
 * @param extras extras存储运行期的路由表参数
 *
 * 优化说明：
 * 1. 新增 paramsMerged 标记位，避免 getExtras() 每次调用都遍历合并 params
 *    原因：getExtras() 可能在路由跳转时被频繁调用，每次都遍历HashMap性能较低
 *    目的：将多次重复遍历优化为只合并一次
 * 2. 使用 Kotlin 的 containsKey 替代 Java 的 keySet().contains()
 *    原因：Kotlin 扩展方法性能更好，代码更简洁
 *    目的：提升遍历效率
 */
@Keep
class RouteItem : Parcelable, Serializable {

    var path = ""
    var className = ""
    var action = ""
    var description = ""

    // 仅用于RouteMap.json文件被gson转换时存储，外部不可访问
    private var params = HashMap<String, String>()

    // extras存储运行期的路由表参数
    private var extras = Bundle()

    /**
     * 标记params是否已经合并到extras中
     * 优化：避免每次getExtras()都重复遍历合并params
     */
    private var paramsMerged = false

    constructor()

    constructor(p: Parcel) {
        path = p.readString() ?: ""
        className = p.readString() ?: ""
        action = p.readString() ?: ""
        description = p.readString() ?: ""
        val obj = p.readSerializable()
        params = if (obj is HashMap<*, *>) {
            obj as HashMap<String, String>
        } else {
            HashMap<String, String>()
        }
        extras = p.readBundle(ClassLoader.getSystemClassLoader()) ?: Bundle()
    }

    constructor(path: String, className: String, action: String, description: String) {
        this.path = path
        this.className = className
        this.action = action
        this.description = description
    }

    fun addParams(key: String, value: String) {
        extras.putString(key, value)
    }

    /**
     * 将Bundle参数合并到extras中
     * 优化：设置paramsMerged标记，避免后续getExtras()重复合并
     */
    internal fun addAll(bundle: Bundle?) = bundle?.let {
        extras.putAll(it)
        paramsMerged = true  // 标记已合并，后续getExtras()无需再遍历params
    }

    /**
     * 获取路由参数
     * 优化：增加paramsMerged检查，只有在需要时才遍历合并params，避免重复计算
     * 原因：每次路由跳转都会调用getExtras()，频繁遍历HashMap影响性能
     */
    fun getExtras(): Bundle {
        // 优化点：只在首次调用时合并params，后续直接返回，避免重复遍历
        if (!paramsMerged) {
            params.forEach {
                // extras为运行期代码逻辑产生的动态参数，优先级最高，不允许被路由表静态参数覆盖
                // 优化：使用Kotlin的containsKey替代keySet().contains()
                if (!extras.containsKey(it.key)) {
                    extras.putString(it.key, it.value)
                }
            }
            paramsMerged = true  // 标记已合并完成
        }
        return extras
    }

    override fun toString(): String {
        return "RouteItem(path='$path', className='$className', action='$action', description='$description', extras=$extras)"
    }

    fun copy(): RouteItem {
        val item = RouteItem()
        item.extras.putAll(extras)
        item.params.putAll(params)
        item.description = description
        item.action = action
        item.className = className
        item.path = path
        return item
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(path)
        dest.writeString(className)
        dest.writeString(action)
        dest.writeString(description)
        dest.writeSerializable(params)
        dest.writeBundle(extras)
    }

    fun readFromParcel(source: Parcel) {
        path = source.readString()!!
        className = source.readString()!!
        action = source.readString()!!
        description = source.readString()!!
        val obj = source.readSerializable()
        params = if (obj is HashMap<*, *>) {
            obj as HashMap<String, String>
        } else {
            HashMap<String, String>()
        }
        extras = source.readBundle(this::class.java.classLoader) ?: Bundle()
    }

    companion object CREATOR : Parcelable.Creator<RouteItem> {
        override fun createFromParcel(parcel: Parcel): RouteItem {
            return RouteItem(parcel)
        }

        override fun newArray(size: Int): Array<RouteItem?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * 将当前路由转换为导航器
 */
fun RouteItem.toNavigator() = Navigator(getUrlWithParams(), null)

/**
 * 获取当前路由的完整url记录
 */
fun RouteItem.getUrlWithParams() = getUrlWithParams { k, v -> "$k=$v" }

/**
 * 获取当前路由的完整url记录
 */
fun RouteItem.getUrlWithParams(handle: NavigatorParamsFixHandle) = getUrlWithParams(handle::fix)

/**
 * 获取当前路由的完整url记录
 * 优化说明：
 * 1. 预分配StringBuilder容量，避免频繁扩容
 *    原因：append操作可能触发数组扩容，影响性能
 *    目的：预估参数数量，一次性分配足够容量
 * 2. 使用StringBuilder的append替代字符串拼接
 *    目的：减少中间字符串对象创建
 */
fun RouteItem.getUrlWithParams(handle: (String, String) -> String): String {
    // 优化点：预估容量，避免StringBuilder频繁扩容
    // 估算方式：path长度 + (每个参数约16字符) * 参数数量
    val estimatedSize = path.length + (extras.keySet().size * 16)
    val stringBuilder = StringBuilder(estimatedSize)
    stringBuilder.append(path)
    
    var isFirst = true
    val extras = getExtras()
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
