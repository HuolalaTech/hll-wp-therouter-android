package com.therouter.app.action;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.therouter.TheRouter;
import com.therouter.app.HomePathIndex;
import com.therouter.app.R;
import com.therouter.demo.base.BaseActivity;
import com.therouter.router.Route;
import com.therouter.router.action.interceptor.ActionInterceptor;

@Route(path = HomePathIndex.DEMO_ACTION_MANAGER)
public class TestActionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.action);
        setTitle("ActionManager 演示");

        TheRouter.addActionInterceptor(HomePathIndex.ACTION, new ActionInterceptor() {
            @Override
            public boolean handle(@NonNull Context context, @NonNull Bundle args) {
                Toast.makeText(context, "收到参数" + args.getString("key"), Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 导航也是兼容action事件的，如果path为一个action事件，则会自动响应action()方法
                // 但如果一个path，既是action事件，又是一个路由页面，则优先响应路由事件
                // 所以如果你希望同时兼容 导航 和Action，则应该使用navigation()方法
                TheRouter.build(HomePathIndex.ACTION).navigation();
            }
        });
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.ACTION).withString("key", "value").action();
                // 如果传入了Content，则接收处的context参数为此处传入的，否则为Application
                // .action(v.getContext());
            }
        });
        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.ACTION_TOAST).action();
                // 如果传入了Content，则接收处的context参数为此处传入的，否则为Application
                // .action(v.getContext());
            }
        });
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.ACTION_TOAST2).action();
                // 如果传入了Content，则接收处的context参数为此处传入的，否则为Application
                // .action(v.getContext());
            }
        });
        findViewById(R.id.button5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.ACTION2).action();
                // 如果传入了Content，则接收处的context参数为此处传入的，否则为Application
                // .action(v.getContext());
            }
        });
    }
}