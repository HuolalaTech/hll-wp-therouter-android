package io.github.flyjingfish.easy_register.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.github.flyjingfish.easy_register.bean.WovenClass
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap


object JsonUtils {
    private val gson: Gson = GsonBuilder().create()
    fun getJson4Str(jsons:List<String>):List<WovenClass>{
        val listType: Type = object : TypeToken<List<WovenClass>>() {}.type
        val searchClassList= mutableListOf<WovenClass>()
        for (json in jsons) {
            val list: List<WovenClass> = gson.fromJson(json, listType)
            searchClassList.addAll(list)
        }
        for (wovenClass in searchClassList) {
            if (wovenClass.searchClass.classNames == null){
                wovenClass.searchClass.classNames = ConcurrentHashMap()
            }
        }
        return searchClassList
    }


}