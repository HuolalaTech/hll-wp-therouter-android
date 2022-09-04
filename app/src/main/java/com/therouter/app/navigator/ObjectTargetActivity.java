package com.therouter.app.navigator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.therouter.app.HomePathIndex;
import com.therouter.app.R;
import com.therouter.router.OnRouteMapChangedListener;
import com.therouter.router.Route;
import com.therouter.TheRouter;
import com.therouter.router.RouteItem;
import com.therouter.router.RouteMapKt;
import com.therouter.router.Autowired;

@Route(path = HomePathIndex.OBJECT_TEST)
@Route(path = HomePathIndex.OBJECT_TEST2)
public class ObjectTargetActivity extends AppCompatActivity {

    @Autowired
    Runnable callback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.callback);
        TheRouter.inject(this);

        final TextView textView = findViewById(R.id.textview);
        RouteMapKt.setOnRouteMapChangedListener(new OnRouteMapChangedListener() {
            @Override
            public void onChanged(@NonNull RouteItem newRouteItem) {
                textView.setText("当前线程" + Thread.currentThread().getName() + "，新添加：\n" + newRouteItem.toString());
            }
        });

        Button button3 = findViewById(R.id.button3);
        button3.setText("修改路由表，把当前页面的path改为首页");
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RouteMapKt.addRouteItem(new RouteItem(HomePathIndex.OBJECT_TEST2, NavigatorTestActivity.class.getName(), "", "测试替换"));
            }
        });
        Button button4 = findViewById(R.id.button4);
        button4.setText("还原上面的修改");
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RouteMapKt.addRouteItem(new RouteItem(HomePathIndex.OBJECT_TEST2, ObjectTargetActivity.class.getName(), "", "还原"));
            }
        });
        Button button5 = findViewById(R.id.button5);
        button5.setText("调用前一个页面的回调对象");
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.run();
                }
            }
        });
    }
}