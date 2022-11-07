package com.therouter.demo.shell;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.therouter.TheRouter;
import com.therouter.businessa.R;
import com.therouter.demo.BusinessAPathIndex;
import com.therouter.demo.di.IRingReferenceTest;
import com.therouter.demo.di.ITest0;
import com.therouter.demo.di.ITest1;
import com.therouter.demo.di.ITestClassAnnotation;
import com.therouter.inject.RecyclerBin;
import com.therouter.inject.RouterInject;
import com.therouter.router.Route;

/**
 * Created by ZhangTao on 17/8/15.
 */
@Route(path = BusinessAPathIndex.INJECT_TEST3)
public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.business_a_test);

        final TextView textView = (TextView) findViewById(R.id.textview);

        final Button button = (Button) findViewById(R.id.button);
        button.setText("插入ITest0");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.append("\n取得对象：" + TheRouter.get(ITest0.class));
            }
        });
        final Button button1 = (Button) findViewById(R.id.button1);
        button1.setText("插入ITest1");
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.append("\n取得对象：" + TheRouter.get(ITest1.class));
            }
        });
        final Button button2 = (Button) findViewById(R.id.button2);
        button2.setText("打印LRU");
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Field field = RouterInject.class.getDeclaredField("mRecyclerBin");
                    field.setAccessible(true);
                    RecyclerBin recyclerBin = (RecyclerBin) field.get(TheRouter.getRouterInject());
                    Method method = RecyclerBin.class.getDeclaredMethod("debug");
                    method.setAccessible(true);
                    textView.setText((String) method.invoke(recyclerBin));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        final Button button3 = (Button) findViewById(R.id.button3);
        button3.setText("模拟循环依赖（直接堆栈溢出崩溃）");
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //传入不同的params对象，会得到不同的TheRouter返回值
                textView.append("\n取得对象：" + TheRouter.get(IRingReferenceTest.class));
            }
        });
        final Button button4 = (Button) findViewById(R.id.button4);
        button4.setText("测试类注释，直接返回接口");
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //传入不同的params对象，会得到不同的TheRouter返回值
                textView.append("\n取得对象：" + TheRouter.get(ITestClassAnnotation.class));
            }
        });
    }
}
