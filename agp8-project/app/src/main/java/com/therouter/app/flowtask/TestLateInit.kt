package com.therouter.app.flowtask

import android.content.Context
import android.util.Log
import com.therouter.app.flowtask.lifecycle.FlowTask

@FlowTask(taskName = AppFlowTask.TEST_LATE_1)
fun init(ctx: Context) {
    Log.i("AppFlowTask.TEST_LATE_1", "packageName:${ctx.packageName},这是延迟初始化的代码1")
}