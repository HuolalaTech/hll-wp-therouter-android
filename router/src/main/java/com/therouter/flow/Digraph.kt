package com.therouter.flow

import android.text.TextUtils
import com.therouter.debug
import com.therouter.require
import java.lang.StringBuilder
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.ArrayList

class Digraph {
    private val tasks: MutableMap<String, Task> = HashMap()
    private val virtualTasks: MutableMap<String, Task> = HashMap()
    private val todoList: MutableList<Task> = CopyOnWriteArrayList()

    private val pendingTaskRunnableList = CopyOnWriteArrayList<Runnable>()

    @Volatile
    var inited = false
        private set

    fun addTask(task: Task?) {
        require(task != null, "Digraph", "Task is Null")
        require(!TextUtils.isEmpty(task?.taskName), "Digraph", "Task name is Empty ${task?.javaClass?.name}")

        task?.taskName?.let {
            if (!tasks.containsKey(it)) {
                tasks[it] = task
            }
        }
    }

    fun addPendingRunnable(r: Runnable) = pendingTaskRunnableList.add(r)

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

    fun getVirtualTask(name: String) = virtualTasks[name]

    fun getDepends(root: Task): Set<Task> {
        val set: MutableSet<Task> = HashSet()
        val dependencies = root.dependencies
        for (key in dependencies) {
            val task = tasks[key]
            // 不存在的Task，有可能是手误写错的，也可能是业务节点Task，全部构建为 VirtualFlowTask，等待手动触发
            if (task == null) {
                virtualTasks[key] = VirtualFlowTask(key)
            } else {
                set.add(task)
            }
        }
        return set
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