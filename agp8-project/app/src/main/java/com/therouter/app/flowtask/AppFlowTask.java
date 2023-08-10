package com.therouter.app.flowtask;

import com.therouter.demo.BusinessBaseFlowTask;
import com.therouter.flow.TheRouterFlowTask;

public interface AppFlowTask extends TheRouterFlowTask, BusinessBaseFlowTask {
    String TEST_APP1 = "app1";
    String TEST_APP2 = "app2";
    String TEST_APP3 = "app3";
    String TEST_APP4 = "app4";
    String TEST_LATE_1 = "late1";
    String TEST_BUSINESS_LATE = "business_demo";
    String CONSENT_AGREEMENT = "consent_to_privacy_agreement";
}
