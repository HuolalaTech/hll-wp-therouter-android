package com.therouter.demo.base;

import android.content.Context;

import com.therouter.app.flowtask.lifecycle.FlowTask;
import com.therouter.demo.BusinessBaseFlowTask;


public class Application {
    @FlowTask(taskName = BusinessBaseFlowTask.BIZBASE_LATE)
    public static void late(Context context) {
        System.out.println("late init in bussiness-base");
    }
}
