package io.github.flyjingfish.easy_register.bean

import java.util.concurrent.ConcurrentHashMap


data class SearchClass(
    val regex: String,
    val extendsClass: String,
    val callType: String,
    val callClass: String,
    val callMethod: String,
    val callMethodValue: String,
    val useType: String,
    var classNames : ConcurrentHashMap<String,MutableSet<String>>? = null
){

    fun getClassNames():MutableSet<String>{
        val list = mutableSetOf<String>()
        classNames?.forEach { (_, value) ->
            list.addAll(value)
        }
        return list
    }

    fun addClass(moduleName:String,className:String){
        classNames?.let {
            val list = it.computeIfAbsent(moduleName) { mutableSetOf() }
            synchronized(list){
                list.add(className)
            }
        }
    }

    fun clear(moduleName:String){
        classNames?.let {
            val list = it.computeIfAbsent(moduleName) { mutableSetOf() }
            synchronized(list){
                list.clear()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchClass

        if (regex != other.regex) return false
        if (extendsClass != other.extendsClass) return false
        if (callType != other.callType) return false
        if (callClass != other.callClass) return false
        if (callMethod != other.callMethod) return false
        if (callMethodValue != other.callMethodValue) return false
        if (useType != other.useType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = regex.hashCode()
        result = 31 * result + extendsClass.hashCode()
        result = 31 * result + callType.hashCode()
        result = 31 * result + callClass.hashCode()
        result = 31 * result + callMethod.hashCode()
        result = 31 * result + callMethodValue.hashCode()
        result = 31 * result + useType.hashCode()
        return result
    }

}