package com.therouter.flow

import android.text.TextUtils
import com.therouter.debug
import com.therouter.require
import java.lang.StringBuilder
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.ArrayList

/**
 * 用于构建有向图，防止Task出现循环依赖的情况<br>
 * 详细逻辑请见官网文档：https://therouter.cn/docs/2022/08/26/01
 */
class Digraph {
    private val tasks = HashMap<String, Task>()

    // virtualTask 不能由有向图调度，需要外部主动调度
    private val virtualTasks = HashMap<String, VirtualFlowTask>()
    private val todoList = CopyOnWriteArrayList<Task>()

    private val pendingTaskRunnableList = CopyOnWriteArrayList<Runnable>()

    @Volatile
    var inited = false
        private set

    /**
     * 向有向图中加入Task
     */
    fun addTask(task: Task?) {
        require(task != null, "FlowTask", "Task is Null")
        require(!TextUtils.isEmpty(task?.taskName), "FlowTask", "Task name is Empty ${task?.javaClass?.name}")
        debug("FlowTask", "FlowTask addTask ${task?.taskName}")

        task?.taskName?.let {
            if (!tasks.containsKey(it)) {
                tasks[it] = task
            }
        }
    }

    /**
     * 由于initSchedule执行比较耗时需要放到异步，而Before需要在路由表初始化之前执行，需要同步
     * 所以单独列出一个方法，检测dependsOn只有beforTheRouterInit的任务，提前执行
     */
    fun beforeSchedule() {
        val virtualFlowTask = getVirtualTask(TheRouterFlowTask.BEFORE_THEROUTER_INITIALIZATION)
        virtualTasks[TheRouterFlowTask.BEFORE_THEROUTER_INITIALIZATION] = virtualFlowTask
        virtualFlowTask.run()

        tasks.values.forEach {
            if (!it.async && it.dependencies.size == 1
                && it.dependencies.contains(TheRouterFlowTask.BEFORE_THEROUTER_INITIALIZATION)
            ) {
                // 此时一定在主线程，所以直接调用
                it.run()
            }
        }
    }

    /**
     * 待执行的task
     */
    fun addPendingRunnable(r: Runnable) = pendingTaskRunnableList.add(r)

    /**
     * 初始化方法
     */
    fun initSchedule() {
        for (task in tasks.values) {
            fillTodoList(task)
        }
        inited = true
        pendingTaskRunnableList.forEach {
            it.run()
        }
    }

    private val loopDependStack: MutableList<Task> = ArrayList()
    private fun fillTodoList(root: Task) {
        if (!root.isDone()) {
            val dependsSet = getDepends(root)
            if (isNotEmpty(dependsSet)) {
                if (loopDependStack.contains(root)) {
                    throw IllegalArgumentException(
                        "TheRouter::Digraph::Cyclic dependency " + getLog(
                            loopDependStack,
                            root
                        )
                    )
                }
                loopDependStack.add(root)
                for (depend in dependsSet) {
                    fillTodoList(depend)
                }
                loopDependStack.remove(root)
                if (!todoList.contains(root)) {
                    todoList.add(root)
                }
            } else {
                if (!todoList.contains(root)) {
                    todoList.add(root)
                }
            }
        }
    }

    /**
     * 开始调度，依次执行无依赖的task，详细逻辑请见官网文档：https://therouter.cn/docs/2022/08/26/01
     */
    fun schedule() {
        for (task in todoList) {
            if (task.isNone()) {
                var allDependenciesDone = true
                task.dependencies.forEach {
                    var dependencyTask = tasks[it]
                    //如果任务池内没有这个任务名，则考虑是否为自定义业务节点
                    if (dependencyTask == null) {
                        dependencyTask = virtualTasks[it]
                    }
                    // 理论上不可能有空的情况
                    if (dependencyTask != null && !dependencyTask.isDone()) {
                        allDependenciesDone = false
                    }
                }

                // 当前task依赖的task全部完成
                if (allDependenciesDone) {
                    debug("FlowTask", "do flow task:" + task.taskName)
                    task.run()
                }
            }
        }
    }

    /**
     * VirtualTask 执行完后调用此方法，用于通知其他依赖此 VirtualTask 的其他 Task 执行
     */
    fun onVirtualTaskDoneListener(name: String) {
        virtualTasks.values.forEach {
            if (it.dependencies.contains(name)) {
                it.dependTaskStatusChanged()
            }
        }
    }

    /**
     * 通过taskName获取一个虚拟task，如果不存在，则创建一个新的虚拟task
     */
    fun getVirtualTask(name: String) = virtualTasks[name] ?: let {
        val vtask = makeVirtualFlowTask(name)
        virtualTasks[name] = vtask
        vtask
    }

    /**
     * 返回入参 Task 的依赖 Task
     */
    fun getDepends(root: Task): Set<Task> {
        val set: MutableSet<Task> = HashSet()
        val dependencies = root.dependencies
        for (key in dependencies) {
            val task = tasks[key]
            // 不存在的Task，有可能是手误写错的，也可能是业务节点Task，全部构建为 VirtualFlowTask，等待手动触发
            if (task == null) {
                virtualTasks[key] = makeVirtualFlowTask(key)
            } else {
                set.add(task)
            }
        }
        return set
    }

    private fun makeVirtualFlowTask(name: String) = when (name) {
        TheRouterFlowTask.BEFORE_THEROUTER_INITIALIZATION -> VirtualFlowTask(name)
        TheRouterFlowTask.THEROUTER_INITIALIZATION -> VirtualFlowTask(
            TheRouterFlowTask.THEROUTER_INITIALIZATION,
            TheRouterFlowTask.BEFORE_THEROUTER_INITIALIZATION
        )

        TheRouterFlowTask.APP_ONSPLASH -> VirtualFlowTask(
            TheRouterFlowTask.APP_ONSPLASH,
            TheRouterFlowTask.THEROUTER_INITIALIZATION
        )

        else -> VirtualFlowTask(name, TheRouterFlowTask.THEROUTER_INITIALIZATION)
    }
}

private fun isNotEmpty(set: Set<Task>?): Boolean {
    return set != null && set.isNotEmpty()
}

private fun getLog(list: List<Task>?, root: Task?): String {
    if (list == null || list.isEmpty()) {
        return ""
    }
    val stringBuilder = StringBuilder()
    for (task in list) {
        stringBuilder.append(task.taskName).append("-->")
    }
    if (root != null) {
        stringBuilder.append(root.taskName)
    }
    return stringBuilder.toString()
}