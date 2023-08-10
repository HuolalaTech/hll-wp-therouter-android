package com.therouter.flow

import androidx.annotation.CallSuper
import com.therouter.TheRouter
import com.therouter.debug
import java.lang.Deprecated

/**
 * 虚拟FlowTask，仅用于自定义业务节点使用。<br>
 * VirtualFlowTask 不能有执行体，只能依赖 VirtualFlowTask。<br>
 * 只要调用run方法，就发起一次可被挂起的执行动作，如果依赖的所有Task都已经执行，就立刻执行，否则挂起等待。<br>
 */
class VirtualFlowTask(taskName: String, dependsOn: String = "") : Task(true, taskName, dependsOn, null) {
    // 当前任务已经可以被执行，需等待依赖的Task是否执行完毕
    @Volatile
    private var ready = false

    /**
     * 依赖 Task 状态有变化后，需要重新查看当前Task是否可以执行了
     */
    fun dependTaskStatusChanged() {
        if (ready) {
            run()
        }
    }

    /**
     * 所有的依赖Task都已经执行完毕
     */
    private fun allReady(): Boolean {
        var allDependTaskReady = true
        dependencies.forEach {
            allDependTaskReady = allDependTaskReady && TheRouter.digraph.getVirtualTask(it).isDone()
        }
        return allDependTaskReady
    }

    @CallSuper
    override fun run() {
        ready = true
        if (state != DONE && allReady()) {
            debug("FlowTask", "Virtual Flow Task $taskName done")
            state = DONE
            TheRouter.digraph.schedule()
            TheRouter.digraph.onVirtualTaskDoneListener(taskName)
        }
    }
}

/**
 * 内部方法，当应用的首个 Activity.onCreate() 执行后自动调用
 */
fun splashInit() = TheRouter.runTask(TheRouterFlowTask.APP_ONSPLASH)

/**
 * 当TheRouter初始化时，执行的FlowTask
 */
fun runInitFlowTask() {
    TheRouter.runTask(TheRouterFlowTask.THEROUTER_INITIALIZATION)
}