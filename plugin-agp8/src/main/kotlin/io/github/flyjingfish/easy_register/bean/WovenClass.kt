package io.github.flyjingfish.easy_register.bean

data class WovenClass(
    val wovenClass: String,
    val wovenMethod: String,
    val searchClass: SearchClass,
    val createWovenClass: Boolean = false
){
    fun clear(moduleName:String){
        searchClass.clear(moduleName)
    }
}