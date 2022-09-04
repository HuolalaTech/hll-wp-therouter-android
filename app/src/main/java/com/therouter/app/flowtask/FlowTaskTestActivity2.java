package com.therouter.app.flowtask;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.therouter.TheRouter;
import com.therouter.app.HomePathIndex;
import com.therouter.app.R;
import com.therouter.router.Route;

@Route(path = HomePathIndex.CONSENT_TO_PRIVACY_AGREEMENT)
public class FlowTaskTestActivity2 extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("FlowTaskExecutor演示");
        setContentView(R.layout.flowtask2);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /// 当用户同意隐私协议时，调度依赖隐私协议的所有任务执行
                TheRouter.runTask(AppFlowTask.CONSENT_AGREEMENT);
            }
        });
    }
}
