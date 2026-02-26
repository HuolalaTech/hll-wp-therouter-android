package com.therouter.app.brick

import com.therouter.TheRouter
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object DataRepository {

    // 数据提供者
    val mapping = HashMap<String, ArrayList<DataProvider<*>>>()

    // 路由定义
    val composeMapping = HashMap<String, HashMap<String, Class<*>>>()

    // path,  params,  priority
    val brickRouteMap = HashMap<String, HashMap<String, ArrayList<DataProvider<*>>>>()


    fun brickCheck() {
        mapping.keys.forEach { path ->
            brickRouteMap.put(path, HashMap<String, ArrayList<DataProvider<*>>>())
        }
        composeMapping.keys.forEach { path ->
            brickRouteMap.put(path, HashMap<String, ArrayList<DataProvider<*>>>())
        }

        brickRouteMap.keys.forEach { path ->
            val priorityList = mapping[path]
            val paramsMap = composeMapping[path]

            var map = brickRouteMap.get(path)
            if (map == null) {
                map = HashMap<String, ArrayList<DataProvider<*>>>()
            }

            priorityList?.forEach { provider ->
                var list = map.get(provider.fieldName)
                if (list == null) {
                    list = ArrayList<DataProvider<*>>()
                }
                val clazz = paramsMap?.get(provider.fieldName)
                if (clazz != null && provider.returnType.isAssignableFrom(clazz)) {
                    list.add(provider)
                } else {
                    // 路由定义上只有一个参数，且类型与数据源声明一致时，直接覆盖数据源声明的fieldName
                    if (paramsMap?.keys?.size == 1) {
                        paramsMap.keys.forEach { k ->
                            val clazz = paramsMap[k]
                            if (clazz != null && provider.returnType.isAssignableFrom(clazz)) {
                                provider.fieldName = k
                                list.add(provider)
                            }
                        }
                    } else {
                        TheRouter.logCat.invoke(
                            "TheRouter::Brick",
                            "${provider.returnType.name} is not ${clazz?.name}"
                        )
                    }
                }
                list.sortBy { a -> a.priority }
                map[provider.fieldName] = list
            }
            brickRouteMap[path] = map
        }

        mapping.clear()
        composeMapping.clear()
    }
}