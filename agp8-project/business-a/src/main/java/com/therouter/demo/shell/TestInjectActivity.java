package com.therouter.demo.shell;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.therouter.TheRouter;
import com.therouter.businessa.R;
import com.therouter.demo.BusinessAPathIndex;
import com.therouter.demo.di.IUserService;
import com.therouter.router.Route;

@Route(path = BusinessAPathIndex.INJECT_TEST)
public class TestInjectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.business_a_inject);

        TextView textview1 = findViewById(R.id.textview1);
        textview1.setText("测试获取用户信息服务：" + TheRouter.get(IUserService.class).getUserInfo());
    }
}