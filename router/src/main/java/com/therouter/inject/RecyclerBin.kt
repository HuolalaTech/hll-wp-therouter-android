package com.therouter.inject

import java.util.*

/**
 * Created by ZhangTao on 17/8/1.
 */
class RecyclerBin internal constructor() {
    private val singletonMap = HashMap<ClassWrapper<*>, Any>()
    private val mCacher = RecyclerLruCache<ClassWrapper<*>?, Any?>(MAX_SIZE).apply {
        setOnEntryRemovedListener { key, oldValue, _ -> m2ndCacher[key] = oldValue }
    }

    private val m2ndCacher = WeakHashMap<ClassWrapper<*>, Any?>()
    fun <T> put(clazz: Class<T>, t: T, vararg params: Any?): T? {
        val key: ClassWrapper<*> = ClassWrapper(clazz, *params)
        return when {
            clazz.isAnnotationPresent(Singleton::class.java) -> {
                synchronized(singletonMap) {
                    return when {
                        singletonMap.containsKey(key) -> {
                            singletonMap[key] as T?
                        }
                        t != null -> {
                            singletonMap[key] = t
                            t
                        }
                        else -> t
                    }
                }
            }
            clazz.isAnnotationPresent(NewInstance::class.java) -> t
            else -> {
                mCacher.put(key, t)
                t
            }
        }
    }

    operator fun <T> get(clazz: Class<T>, vararg params: Any?): T? {
        val key: ClassWrapper<*> = ClassWrapper(clazz, *params)
        var t = singletonMap[key]
        if (t == null) {
            t = mCacher[key]
            if (t == null) {
                t = m2ndCacher.remove(key)
                if (t != null) {
                    mCacher.put(key, t)
                }
            }
        }
        return t as T?
    }

    /**
     * keep for debug
     */
    private fun debug(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("singletonMap: ").append(singletonMap.size).append("\n")
        for (temp in singletonMap.keys) {
            stringBuilder.append(temp.unWrapper().simpleName).append(" : ")
                .append(singletonMap[temp]!!.javaClass.simpleName).append(" hash:: ")
                .append(singletonMap[temp].hashCode()).append("\n")
        }
        val map = mCacher.snapshot()
        stringBuilder.append("LRU: ").append(mCacher.size()).append("\n")
        for (temp in map.keys) {
            stringBuilder.append(temp?.unWrapper()?.simpleName).append(" : ")
                .append(map[temp]!!.javaClass.simpleName).append(" hash:: ")
                .append(map[temp].hashCode()).append("\n")
        }
        stringBuilder.append("2ndCacher: ").append(m2ndCacher.size).append("\n")
        for (temp in m2ndCacher.keys) {
            if (m2ndCacher[temp] == null) {
                stringBuilder.append(temp.unWrapper().simpleName).append(" recycled")
            } else {
                val obj = m2ndCacher[temp]
                stringBuilder.append(temp.unWrapper().simpleName).append(" : ")
                    .append(obj!!.javaClass.simpleName).append(" hash:: ")
                    .append(obj.hashCode()).append("\n")
            }
        }
        com.therouter.debug("RecyclerBin", stringBuilder.toString())
        return stringBuilder.toString()
    }
}

private const val MAX_SIZE = 10
