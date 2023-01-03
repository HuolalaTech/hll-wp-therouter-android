package com.therouter.app;

import com.therouter.demo.BusinessAPathIndex;
import com.therouter.demo.BusinessBPathIndex;

public interface HomePathIndex extends BusinessAPathIndex, BusinessBPathIndex {

    String DEMO_NAVIGATOR = "http://kymjs.com/therouter/demo_navigator";
    String DEMO_SERVICE_PROVIDER = INJECT_TEST2;
    String DEMO_FLOW_TASK_EXECUTOR = "http://kymjs.com/therouter/demo_flow_task_executor";
    String DEMO_ACTION_MANAGER = "http://kymjs.com/therouter/demo_action_manager";
    String DEMO_OTHER = "http://kymjs.com/therouter/demo_other";

    String DEMO_HISTORY = "http://kymjs.com/therouter/demo_history";

    String PENDING = "http://kymjs.com/therouter/pending";
    String KOTLIN = "http://kymjs.com/therouter/test";
    String KOTLIN2 = "http://kymjs.com/therouter/test2";
    String HOME = "http://kymjs.com/therouter/home";
    String HOME2 = "http://kymjs.com/therouter/home2";
    String OBJECT_TEST = "http://kymjs.com/therouter/callbacktest";
    String OBJECT_TEST2 = "http://kymjs.com/therouter/callbacktest2";
    String INTERCEPTOR = "http://kymjs.com/therouter/interceptor";

    String CONSENT_TO_PRIVACY_AGREEMENT = "http://kymjs.com/therouter/consent_to_privacy_agreement";

    String ACTION = "action://action_test";
    String ACTION2 = "action://action_test2";
}
