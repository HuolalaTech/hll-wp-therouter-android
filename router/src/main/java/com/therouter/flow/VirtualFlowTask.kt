package com.therouter.flow

import androidx.annotation.CallSuper
import com.therouter.TheRouter
import com.therouter.debug

/**
 * 虚拟FlowTask，仅用于自定义业务节点使用。
 * VirtualFlowTask 不能有执行体，不能依赖其他Task，只要调用run方法，就认为执行成功
 */
class VirtualFlowTask(taskName: String) : Task(true, taskName, "", null) {
    @CallSuper
    override fun run() {
        if (state != DONE) {
            debug("FlowTask", "Virtual Flow Task $taskName done")
            state = DONE
            TheRouter.digraph.schedule()
        }
    }
}

fun splashInit() = TheRouter.runTask(TheRouterFlowTask.APP_ONSPLASH)

fun applicationCreate() = TheRouter.runTask(TheRouterFlowTask.APP_ONCREATE)