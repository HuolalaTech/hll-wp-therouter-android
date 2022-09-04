package com.therouter;

import android.content.Context;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import com.therouter.app.HomePathIndex;
import com.therouter.demo.di.ITest;
import com.therouter.router.RouteMapKt;
import com.therouter.router.RouterMapInitTask;

public class App extends MultiDexApplication {
    @Override
    protected void attachBaseContext(Context base) {
        TheRouter.setDebug(true);
        super.attachBaseContext(base);
        RouteMapKt.setRouteMapInitTask(new RouterMapInitTask() {
            @Override
            public void asyncInitRouteMap() {
                // 什么也不做，应该也是有默认路由的
                Log.d("testapp", "执行默认初始化逻辑");
            }
        });

        // 测试路由表还未初始化时就跳转路由
        TheRouter.build(HomePathIndex.PENDING).navigation();

        // 测试服务提供者还未注册就被使用者调用
        ITest iTest = TheRouter.get(ITest.class);
        // 此时一定是空，因为还没有初始化
        if (iTest != null) {
            Log.d("testapp", "调用");
        } else {
            Log.d("testapp", "TheRouter.get(ITest.class)为空");
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
