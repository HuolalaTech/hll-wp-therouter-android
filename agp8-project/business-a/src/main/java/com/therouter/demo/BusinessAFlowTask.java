package com.therouter.demo;

import com.therouter.flow.TheRouterFlowTask;

public interface BusinessAFlowTask extends TheRouterFlowTask, BusinessBaseFlowTask {
    String BIZA_INTERCEPTOR = "businessA_interceptor";
    String BIZA_INIT1 = "businessA_init1";
    String BIZA_INIT2 = "businessA_init2";
    String BIZA_INIT3 = "businessA_init3";
}
