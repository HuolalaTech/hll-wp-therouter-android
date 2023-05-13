package com.therouter.flow

import com.therouter.TheRouter
import com.therouter.debug
import com.therouter.execute
import com.therouter.executeInMainThread
import com.therouter.history.FlowTaskHistory
import com.therouter.history.pushHistory
import java.util.*

const val NONE = 0
const val DONE = 2
const val RUNNING = 1

/**
 * 所有Task都有一个共同的父依赖：THEROUTER_INITIALIZATION
 *
 * @param async 是否在异步执行此 task
 * @param taskName 任务名
 * @param dependsOn 此 task 依赖的任务，如果有多个，用英文逗号隔开，例如 "mmkv,init"
 * @param dependsOn 此 task 的执行内容
 */
open class Task(
    val async: Boolean,
    val taskName: String,
    dependsOn: String,
    private val runnable: Runnable?
) {
    @Volatile
    protected var state = NONE

    /**
     *  此 task 依赖的任务集合
     */
    val dependencies = HashSet<String>()

    init {
        dependsOn.split(",").forEach {
            if (it.isNotBlank()) {
                dependencies.add(it.trim())
            }
        }
        if (dependencies.contains(taskName)) {
            throw IllegalArgumentException("TheRouter::Task::The task cannot depend on himself : $taskName")
        }
        // 所有Task都有一个共同的父依赖：THEROUTER_INITIALIZATION
        if (dependencies.isEmpty()
            && taskName != TheRouterFlowTask.THEROUTER_INITIALIZATION
            && taskName != TheRouterFlowTask.BEFORE_THEROUTER_INITIALIZATION
        ) {
            dependencies.add(TheRouterFlowTask.THEROUTER_INITIALIZATION)
        }
    }

    internal open fun run() {
        if (isNone()) {
            synchronized(this) {
                if (isNone()) {
                    state = RUNNING
                    val logMsg = "Task $taskName on ${
                        if (async) {
                            "Async"
                        } else {
                            "Main"
                        }
                    }Thread${
                        if (runnable is FlowTaskRunnable) {
                            " Exec ${runnable.log()}."
                        } else {
                            "."
                        }
                    }"
                    debug("FlowTask", logMsg)
                    pushHistory(FlowTaskHistory(logMsg))
                    if (async) {
                        execute {
                            runnable?.run()
                            state = DONE
                            TheRouter.digraph.schedule()
                        }
                    } else {
                        executeInMainThread {
                            runnable?.run()
                            state = DONE
                            TheRouter.digraph.schedule()
                        }
                    }
                }
            }
        }
    }

    internal fun isNone() = state == NONE

    internal fun isRunning() = state == RUNNING

    internal fun isDone() = state == DONE
}