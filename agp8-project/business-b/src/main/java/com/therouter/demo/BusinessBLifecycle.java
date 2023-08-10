package com.therouter.demo;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.therouter.TheRouter;
import com.therouter.TheRouterThreadPool;
import com.therouter.app.flowtask.lifecycle.FlowTask;
import com.therouter.router.Navigator;
import com.therouter.router.action.interceptor.ActionInterceptor;

public class BusinessBLifecycle {

    /**
     * 延迟初始化，当首个Activity被启动后才会回调
     *
     * @param context application
     */
    @FlowTask(taskName = BusinessBFlowTask.BIZB_INTERCEPTOR, dependsOn = BusinessBaseFlowTask.APP_ONSPLASH)
    public static void test3(Context context) {
        TheRouter.addActionInterceptor(BusinessBPathIndex.ACTION_TOAST, new ActionInterceptor() {
            @Override
            public boolean handle(@NonNull Context context, @NonNull Navigator navigator) {
                super.handle(context, navigator);
                TheRouterThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        TheRouterThreadPool.executeInMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "业务B弹出，return false", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                return false;
            }
        });
        TheRouter.addActionInterceptor(BusinessBPathIndex.ACTION_TOAST2, new ActionInterceptor() {
            @Override
            public int getPriority() {
                return 100;
            }

            @Override
            public boolean handle(@NonNull Context context, @NonNull Navigator navigator) {
                super.handle(context, navigator);
                Log.d("debug", "业务B弹出");
                Toast.makeText(context, "业务B弹出，优先级最高，return true，阻断其他响应", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }
}
