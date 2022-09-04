package com.therouter.app.flowtask;

import android.content.Context;

import com.therouter.app.flowtask.lifecycle.FlowTask;

public class TestLifecycle {

    /**
     * 当Application onCreate时会回调该方法
     *
     * @param context application
     */
    @FlowTask(taskName = AppFlowTask.TEST_APP1, dependsOn = AppFlowTask.APP_ONCREATE)
    public static void test1(Context context) {
        System.out.println("===主应用内=====onApplicationCreate");
    }

    /**
     * 将会在异步执行
     */
    @FlowTask(taskName = AppFlowTask.TEST_APP2, dependsOn = AppFlowTask.APP_ONCREATE, async = true)
    public static void test2(Context context) {
        System.out.println("异步===主应用内======Application onCreate后执行");
    }

    /**
     * 将会在app1和app2任务执行以后执行
     */
    @FlowTask(taskName = AppFlowTask.TEST_APP3, dependsOn = AppFlowTask.TEST_APP1 + "," + AppFlowTask.TEST_APP2, async = true)
    public static void test3(Context context) {
        System.out.println("异步===主应用内======在app1和app2后");
    }

    /**
     * 将会在首个Activity.oncreate执行以后异步执行
     */
    @FlowTask(taskName = AppFlowTask.TEST_APP4, dependsOn = AppFlowTask.APP_ONSPLASH, async = true)
    public static void test4(Context context) {
        System.out.println(Thread.currentThread().getName() + "线程名===主应用内=，在splash activity后");
    }

    @FlowTask(taskName = AppFlowTask.MMKV)
    public static void test5(Context context) {
        System.out.println("FlowTask初始化mmkv，无先后要求");
    }

    @FlowTask(taskName = AppFlowTask.CONFIG, dependsOn = AppFlowTask.MMKV)
    public static void test6(Context context) {
        System.out.println("FlowTask自动初始化config，在mmkv后");
    }

    @FlowTask(taskName = AppFlowTask.LOGIN, dependsOn = AppFlowTask.CONFIG)
    public static void logintest7(Context context) {
        System.out.println("FlowTask自动初始化login，在config后");
    }
}
