package com.therouter.app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.therouter.TheRouter;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setTitle("TheRouter功能介绍");
        // 如果用到了 @Autowired 注解，需要加这一行，这一行直接写在BaseActivity中更好
        // 如果用到了onNewIntent()，也需要调用这一行，并且在调用前需要将新intent 重新set一遍
//        TheRouter.inject(this);

        TextView textView = findViewById(R.id.content_version);
        try {
            Class<?> aClass = Class.forName("com.therouter.BuildConfig");
            Object version = aClass.getField("VERSION").get(null);
            textView.setText("当前SDK版本：" + version.toString());
        } catch (Exception e) {
            textView.setText("当前SDK版本：源码依赖");
        }

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.DEMO_NAVIGATOR)
                        .withInAnimation(R.anim.activity_slide_in)
                        .withOutAnimation(R.anim.activity_slide_out)
                        // 加动画以后，必须传入activity对象，否则debug环境抛异常，release动画不生效
                        .navigation(v.getContext());
            }
        });
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.DEMO_SERVICE_PROVIDER)
                        .withInAnimation(R.anim.activity_slide_in)
                        .withOutAnimation(R.anim.activity_slide_out)
                        // 加动画以后，必须传入activity对象，否则debug环境抛异常，release动画不生效
                        .navigation(v.getContext());
            }
        });
        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.DEMO_FLOW_TASK_EXECUTOR)
                        .withInAnimation(R.anim.activity_slide_in)
                        .withOutAnimation(R.anim.activity_slide_out)
                        // 加动画以后，必须传入activity对象，否则debug环境抛异常，release动画不生效
                        .navigation(v.getContext());
            }
        });
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.DEMO_ACTION_MANAGER)
                        .withInAnimation(R.anim.activity_slide_in)
                        .withOutAnimation(R.anim.activity_slide_out)
                        // 加动画以后，必须传入activity对象，否则debug环境抛异常，release动画不生效
                        .navigation(v.getContext());
            }
        });
        findViewById(R.id.button5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.DEMO_OTHER).navigation();
            }
        });


    }
}
