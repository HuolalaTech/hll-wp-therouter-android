package com.therouter.app.flowtask;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.therouter.TheRouter;
import com.therouter.app.HomePathIndex;
import com.therouter.app.R;
import com.therouter.app.flowtask.lifecycle.FlowTask;
import com.therouter.router.Route;

@Route(path = HomePathIndex.DEMO_FLOW_TASK_EXECUTOR)
public class FlowTaskTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("FlowTaskExecutor演示");
        setContentView(R.layout.flowtask);

        /**
         * 更多自启动任务定义，请查看{@link TestLifecycle}
         */
        //更多自启动任务定义，请查看{@link TestLifecycle}
        //更多自启动任务定义，请查看{@link TestLifecycle}
        //更多自启动任务定义，请查看{@link TestLifecycle}
        //更多自启动任务定义，请查看{@link TestLifecycle}

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.CONSENT_TO_PRIVACY_AGREEMENT).navigation();
            }
        });
    }

    /**
     * 自定义一个初始化task，他会在同意隐私协议以后，被自动调用
     * AppFlowTask.CONSENT_AGREEMENT 的定义请查看：{@link FlowTaskTestActivity}
     */
    @FlowTask(taskName = AppFlowTask.TEST_BUSINESS_LATE, dependsOn = AppFlowTask.CONSENT_AGREEMENT)
    public static void init(Context context) {
        Toast.makeText(context, "用户已同意隐私协议，自动初始化相关依赖", Toast.LENGTH_SHORT).show();
    }
}
