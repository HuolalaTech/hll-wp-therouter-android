package com.therouter.flow;

/**
 * 内置初始化节点定义
 */
public interface TheRouterFlowTask {

    /**
     * 当应用的首个 Activity.onCreate() 执行后初始化
     */
    String APP_ONSPLASH = "TheRouter_activity_splash";

    /**
     * 当应用启动后，在TheRouter初始化之前，执行任务。
     *
     * @since 1.1.1-rc2
     */
    String THEROUTER_INITIALIZATION = "TheRouter_Initialization";

    /**
     * 当TheRouter初始化后，执行相关依赖任务。
     *
     * @since 1.1.2-rc6
     */
    String BEFORE_THEROUTER_INITIALIZATION = "TheRouter_Before_Initialization";
}
