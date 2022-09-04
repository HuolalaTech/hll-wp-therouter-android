package com.therouter.app;

import android.os.Bundle;
import android.view.View;

import com.therouter.TheRouter;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setTitle("功能介绍");
        // 如果用到了 @Autowired 注解，需要加这一行，这一行直接写在BaseActivity中更好
//        TheRouter.inject(this);

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.DEMO_NAVIGATOR).navigation();
            }
        });
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.DEMO_SERVICE_PROVIDER).navigation();
            }
        });
        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.DEMO_FLOW_TASK_EXECUTOR).navigation();
            }
        });
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.DEMO_ACTION_MANAGER).navigation();
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
