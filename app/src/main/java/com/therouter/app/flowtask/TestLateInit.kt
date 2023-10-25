package com.therouter.app.flowtask

import android.content.Context
import android.util.Log
import com.therouter.app.flowtask.lifecycle.FlowTask
import com.therouter.flow.TheRouterFlowTask

@FlowTask(taskName = AppFlowTask.TEST_LATE_1)
fun init(ctx: Context) {
    Log.i("AppFlowTask.TEST_LATE_1", "packageName:${ctx.packageName},这是延迟初始化的代码1")
}

@FlowTask(taskName = AppFlowTask.TEST_LATE_2, dependsOn = TheRouterFlowTask.THEROUTER_INITIALIZATION)
fun init2(ctx: Context) {
    Log.i("AppFlowTask.TEST_LATE_2", "packageName:${ctx.packageName},这是延迟初始化的代码2")
}