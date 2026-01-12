package com.therouter.brick

import androidx.compose.runtime.Composable
import com.therouter.TheRouter
import com.therouter.app.flowtask.lifecycle.FlowTask
import com.therouter.flow.TheRouterFlowTask
import com.therouter.router.Navigator
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object DataRepository {

    // 数据提供者
    val mapping = HashMap<String, ArrayList<DataProvider<*>>>()

    // 路由定义
    val composeMapping = HashMap<String, HashMap<String, Class<*>>>()

    // path,  params,  priority
    val brickRouteMap = HashMap<String, HashMap<String, ArrayList<DataProvider<*>>>>()

    @Composable
    fun makeData(navigator: Navigator, ui: @Composable (key: String, value: Any?) -> Unit) {
        val type = navigator.simpleUrl
        val allField = brickRouteMap.get(type)
        allField?.forEach {
            val fieldName = it.key
            val priorityList = it.value

            priorityList.sortBy { a -> a.priority }
            if (priorityList.isNotEmpty()) {
                priorityList.forEach { provider ->
                    val t = provider.make?.invoke(navigator)
                    if (t != null) {
                        ui.invoke(fieldName, t)
                        return@forEach
                    }
                }
            }
        }
    }
}

@FlowTask(taskName = "Brick_DataRepository_brickCheck", dependsOn = TheRouterFlowTask.APP_ONSPLASH)
fun brickCheck() {
    DataRepository.brickRouteMap.clear()
    DataRepository.mapping.keys.forEach { path ->
        DataRepository.brickRouteMap.put(path, HashMap<String, ArrayList<DataProvider<*>>>())
    }
    DataRepository.composeMapping.keys.forEach { path ->
        DataRepository.brickRouteMap.put(path, HashMap<String, ArrayList<DataProvider<*>>>())
    }

    DataRepository.brickRouteMap.keys.forEach { path ->
        val priorityList = DataRepository.mapping[path]
        val paramsMap = DataRepository.composeMapping[path]

        var map = DataRepository.brickRouteMap.get(path)
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
                TheRouter.logCat.invoke("TheRouter::Brick", "${provider.returnType.name} is not ${clazz?.name}")
            }
            map.set(provider.fieldName, list)
        }
        DataRepository.brickRouteMap.set(path, map)
    }
}
