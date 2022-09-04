package com.therouter.inject

/**
 * Created by ZhangTao on 17/8/15.
 */
class ClassWrapper<T>(private val clazz: Class<T>, vararg params: Any?) {
    var key: String? = null
    fun unWrapper(): Class<T> {
        return clazz
    }

    override fun equals(o: Any?): Boolean {
        return if (o is ClassWrapper<*>) {
            unWrapper() == o.unWrapper() && key == o.key
        } else {
            super.equals(o)
        }
    }

    override fun hashCode(): Int {
        return (unWrapper().hashCode().toString() + "" + (key ?: "").hashCode()).hashCode()
    }

    init {
        params.forEach {
            it?.let {
                key += it.toString()
            }
        }
    }
}