package com.therouter.demo.shell;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.therouter.TheRouter;
import com.therouter.businessa.R;
import com.therouter.demo.BusinessAPathIndex;
import com.therouter.demo.di.ITest7;
import com.therouter.router.Route;

/**
 * Created by ZhangTao on 17/8/28.
 */
@Route(path = BusinessAPathIndex.INJECT_TEST4)
public class MultiThreadActivity extends AppCompatActivity {

    StringBuffer mStringBuffer = new StringBuffer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.business_a_test);
        final TextView textView = (TextView) findViewById(R.id.textview);

        Button button2 = (Button) findViewById(R.id.button2);
        button2.setText("启动测试");
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < 15; i++) {
                    Thread thread1 = new ThreadOne();
                    Thread thread3 = new ThreadThree();
                    Thread thread2 = new ThreadTwo();
                    thread1.start();
                    thread3.start();
                    thread2.start();
                }
            }
        });
        Button button1 = (Button) findViewById(R.id.button1);
        button1.setText("输出记录");
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setText(mStringBuffer);
            }
        });
        Button button = (Button) findViewById(R.id.button);
        button.setText("清空数据");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStringBuffer.delete(0, mStringBuffer.length());
                textView.setText(mStringBuffer);
            }
        });
    }

    class ThreadOne extends Thread {
        @Override
        public void run() {
            super.run();
            ITest7 test7 = TheRouter.get(ITest7.class);
            mStringBuffer.append(getName()).append("::\n").append(test7).append("hash：：").append(test7.hashCode()).append("\n");
            Log.d("therouter", mStringBuffer.toString());
        }
    }

    class ThreadTwo extends Thread {
        @Override
        public void run() {
            super.run();
            ITest7 test7 = TheRouter.get(ITest7.class);
            mStringBuffer.append(getName()).append("::\n").append(test7).append("hash：：").append(test7.hashCode()).append("\n");
            Log.d("therouter", mStringBuffer.toString());
        }
    }

    class ThreadThree extends Thread {
        @Override
        public void run() {
            super.run();
            ITest7 test7 = TheRouter.get(ITest7.class);
            mStringBuffer.append(getName()).append("::\n").append(test7).append("hash：：").append(test7.hashCode()).append("\n");
            Log.d("therouter", mStringBuffer.toString());
        }
    }
}
