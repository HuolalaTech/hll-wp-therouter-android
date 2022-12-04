package com.therouter.demo;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.therouter.TheRouter;
import com.therouter.app.flowtask.lifecycle.FlowTask;
import com.therouter.router.action.interceptor.ActionInterceptor;


public class TestLifecycle {

    /**
     * 当app3执行以后会被执行
     */
    @FlowTask(taskName = BusinessAFlowTask.BIZA_INTERCEPTOR, dependsOn = BusinessAFlowTask.MMKV + "," + BusinessAFlowTask.APP_ONCREATE)
    public static void test1(Context context) {
        System.out.println("=====来自业务模块====Application.Create，mmkv之后执行");

        TheRouter.addActionInterceptor(BusinessAPathIndex.A_ACTION_TOAST, new ActionInterceptor() {
            @Override
            public boolean handle(@NonNull Context context, @NonNull Bundle args) {
                Toast.makeText(context, "业务A弹出，return false", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        TheRouter.addActionInterceptor(BusinessAPathIndex.A_ACTION_TOAST2, new ActionInterceptor() {
            @Override
            public int getPriority() {
                // 数字越大，优先级越高
                return 1;
            }

            @Override
            public boolean handle(@NonNull Context context, @NonNull Bundle args) {
                Log.d("debug", "业务A弹出");
                Toast.makeText(context, "业务A弹出，优先级低，return false", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    @FlowTask(taskName = BusinessAFlowTask.BIZA_INIT1, dependsOn = BusinessAFlowTask.LOGIN, async = true)
    public static void test2(Context context) {
        System.out.println(Thread.currentThread().getName() + "异步====来自业务模块=====login执行以后");
    }

    @FlowTask(taskName = BusinessAFlowTask.BIZA_INIT2, dependsOn = BusinessAFlowTask.LOGIN + "," + BusinessAFlowTask.APP_ONCREATE)
    public static void test3(Context context) {
        System.out.println(Thread.currentThread().getName() + "线程=====来自业务模块====");
    }

    @FlowTask(taskName = BusinessAFlowTask.BIZA_INIT3, async = true)
    public static void test4(Context context) {
        System.out.println(Thread.currentThread().getName() + "线程====来自业务模块=====");
    }
}
