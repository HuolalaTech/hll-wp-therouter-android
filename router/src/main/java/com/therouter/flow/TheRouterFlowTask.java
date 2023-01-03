package com.therouter.flow;

public interface TheRouterFlowTask {
    /**
     * 替换为 TheRouterFlowTask.THEROUTER_INITIALIZATION<br>
     * 将在 1.1.4 版本移除本Task
     */
    @Deprecated
    String APP_ONCREATE = "TheRouter_application_oncreate";

    String APP_ONSPLASH = "TheRouter_activity_splash";
    String THEROUTER_INITIALIZATION = "TheRouter_Initialization";
    String BEFORE_THEROUTER_INITIALIZATION = "TheRouter_Before_Initialization";
}
