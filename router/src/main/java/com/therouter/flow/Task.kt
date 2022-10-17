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

open class Task(
    val async: Boolean,
    val taskName: String,
    dependsOn: String,
    private val runnable: Runnable?
) {
    @Volatile
    protected var state = NONE
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