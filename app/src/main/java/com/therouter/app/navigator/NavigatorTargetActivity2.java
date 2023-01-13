package com.therouter.app.navigator;

import com.therouter.app.HomePathIndex;
import com.therouter.router.Route;

@Route(path = HomePathIndex.HOME2, params = {"strFromAnnotation", "来自注解设置的默认值，允许路由动态修改"})
@Route(path = HomePathIndex.HOME, action = "action://scheme.com",
        description = "路由测试首页", params = {"strFromAnnotation", "来自注解设置的默认值，允许路由动态修改"})
public class NavigatorTargetActivity2 extends NavigatorTargetActivity {
}
