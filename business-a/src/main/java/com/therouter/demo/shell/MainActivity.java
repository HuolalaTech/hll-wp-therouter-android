package com.therouter.demo.shell;

import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.therouter.TheRouter;
import com.therouter.businessa.R;
import com.therouter.demo.BusinessAPathIndex;
import com.therouter.demo.di.ITest;
import com.therouter.demo.di.ITest0;
import com.therouter.demo.di.ITest1;
import com.therouter.demo.di.ITest10;
import com.therouter.demo.di.ITest11;
import com.therouter.demo.di.ITest2;
import com.therouter.demo.di.ITest3;
import com.therouter.demo.di.ITest4;
import com.therouter.demo.di.ITest5;
import com.therouter.demo.di.ITest6;
import com.therouter.demo.di.ITest7;
import com.therouter.demo.di.ITest8;
import com.therouter.demo.di.ITest9;
import com.therouter.demo.di.TestEvent;
import com.therouter.inject.RecyclerBin;
import com.therouter.inject.RouterInject;
import com.therouter.router.Autowired;
import com.therouter.router.Route;


/**
 * Created by ZhangTao on 17/8/11.
 */
@Route(path = BusinessAPathIndex.INJECT_TEST2)
public class MainActivity extends AppCompatActivity {

    // 测试int值传递
    @Autowired
    int intValue;

    @Autowired
    String str_123_Value;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("ServiceProvider 测试用例");
        setContentView(R.layout.business_a_main);
        // 如果用到了 @Autowired 注解，需要加这一行，这一行直接写在BaseActivity中更好
        TheRouter.inject(this);

        final TextView textView = (TextView) findViewById(R.id.textview);
        textView.setText("默认参数已读取：" + intValue + "参数2：" + str_123_Value);

        final Button button0 = (Button) findViewById(R.id.button0);
        button0.setText("手动GC（需要root权限）否则请用命令adb kill -10 [pid]");
        button0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Runtime.getRuntime().exec("su | kill -10 " + Process.myPid());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        final Button button1 = (Button) findViewById(R.id.button1);
        button1.setText("获取ITest，来自当前模块");
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ITest test = TheRouter.get(ITest.class);
                textView.append("\n当前对象：" + test.getClass().getName() + test.hashCode());
            }
        });

        final Button button2 = (Button) findViewById(R.id.button2);
        button2.setText("打印当前LRU");
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
        button3.setText("批量缓存10个对象");
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.get(ITest.class);
                TheRouter.get(ITest0.class).getMessage();
                TheRouter.get(ITest1.class);
                TheRouter.get(ITest2.class);
                TheRouter.get(ITest3.class);
                TheRouter.get(ITest4.class);
                TheRouter.get(ITest5.class, "参数1", v.getContext());
                TheRouter.get(ITest6.class, "参数");
                TheRouter.get(ITest7.class);
                TheRouter.get(ITest8.class);
            }
        });

        final Button button4 = (Button) findViewById(R.id.button4);
        button4.setText("测试多参数(Context)");
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.append("\n打印信息：" + TheRouter.get(ITest1.class, v.getContext()).toString());
            }
        });

        final Button button5 = (Button) findViewById(R.id.button5);
        button5.setText("测试多参数(Context, int)");
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.append("\n多参数测试：" + TheRouter.get(ITest1.class,
                        v.getContext(), (int) (Math.random() * 10)));
            }
        });

        final Button button6 = (Button) findViewById(R.id.button6);
        button6.setText("测试无参数");
        button6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.append("\n打印信息：" + TheRouter.get(ITest1.class).toString());
            }
        });

        final Button button7 = (Button) findViewById(R.id.button7);
        button7.setText("测试单例对象ITest7");
        button7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.append("\n取得对象：" + TheRouter.get(ITest7.class));
            }
        });
        final Button button7_1 = (Button) findViewById(R.id.button7_1);
        button7_1.setText("测试无创建器类");
        button7_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.append("\n取得对象：" + TheRouter.get(TestEvent.class));
            }
        });

        final Button button8 = (Button) findViewById(R.id.button8);
        button8.setText("测试不缓存对象");
        button8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.append("\n取得对象：" + TheRouter.get(ITest9.class));
            }
        });

        final Button button9 = (Button) findViewById(R.id.button9);
        button9.setText("缓存ITest10，ITest11");
        button9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.append("\n取得对象：" + TheRouter.get(ITest10.class));
                textView.append("\n取得对象：" + TheRouter.get(ITest11.class));
            }
        });

        final Button button10 = (Button) findViewById(R.id.button10);
        button10.setText("启动新activity");
        button10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(BusinessAPathIndex.INJECT_TEST3).navigation();
            }
        });
        final Button button11 = (Button) findViewById(R.id.button11);
        button11.setText("测试多线程单例情况");
        button11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(BusinessAPathIndex.INJECT_TEST4).navigation();
            }
        });
    }
}