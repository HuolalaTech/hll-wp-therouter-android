package com.therouter.app.navigator;

import static com.therouter.app.KotlinPathIndex.Test.HOME2;

import android.os.Bundle;
import android.widget.TextView;

import com.therouter.app.HomePathIndex;
import com.therouter.app.R;
import com.therouter.router.Autowired;
import com.therouter.router.Route;

import java.util.ArrayList;

@Route(path = HOME2, params = {"strFromAnnotation", "来自注解设置的默认值，允许路由动态修改"})
@Route(path = HomePathIndex.HOME, action = "action://scheme.com",
        description = "路由测试首页", params = {"strFromAnnotation", "来自注解设置的默认值，允许路由动态修改"})
public class NavigatorTargetActivity2 extends NavigatorTargetActivity<String> {

    @Autowired
    String stringChildClassField;
    @Autowired
    ArrayList<ArrayList<String>> stringChildClassFields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView textView = findViewById(R.id.textview12);
        textView.setText("子类 @Autowired 数据：" + stringChildClassField);
        if (stringChildClassFields != null) {
            textView.append("\n stringChildClassFields: " + stringChildClassFields.get(0).get(0));
        }
    }
}
