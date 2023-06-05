package com.therouter.app.navigator;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.therouter.TheRouter;
import com.therouter.app.HomePathIndex;
import com.therouter.app.R;
import com.therouter.app.router.InternalBeanTest;
import com.therouter.router.Autowired;
import com.therouter.router.Route;

public class NavigatorTargetActivity extends AppCompatActivity {

//    .withInt("intValue", 12345678) // 测试传 int 值
//    .withString("stringIntValue", "12345678")// 测试用 string 传 int 值
//    .withString("str_123_Value", "测试传中文字符串")// 测试 string
//    .withString("boolParseError", "非boolean值") // 测试用 boolean 解析字符串的情况
//    .withString("shortParseError", "12345678") // 测试用 short 解析超长数字的情况
//    .withBoolean("boolValue", true) // 测试 boolean
//    .withLong("longValue", 123456789012345L)  // 测试 long
//    .withChar("charValue", 'c')  // 测试 char
//    .withDouble("double", 3.14159265358972)// 测试double，key与关键字冲突

    // 测试int值传递
    @Autowired
    int intValue;

    @Autowired
    String stringIntValue;

    @Autowired
    String str_123_Value;

    @Autowired
    boolean boolParseError;

    @Autowired
    short shortParseError;

    @Autowired
    boolean boolValue;

    @Autowired
    Long longValue;

    @Autowired
    char charValue;

    @Autowired(name = "double")
    double doubleValue;

    @Autowired
    float floatValue;

    @Autowired
    String strFromAnnotation;  // 来自注解设置的默认值，允许路由动态修改

    // id需要是final变量
    //    @Autowired(id = R.id.button1)
    Button button1;

    @Autowired(name = "SerializableObject")
    InternalBeanTest.RowBean serializableBean;
    @Autowired(name = "ParcelableObject")
    InternalBeanTest.RowBean parcelableBean;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigator_target);
        setTitle("导航落地页1");
        // Autowired注入，这一行应该写在BaseActivity中
        TheRouter.inject(this);

        final TextView textView1 = findViewById(R.id.textview1);
        textView1.setText("接收int值传递：integer2=" + intValue);

        final TextView textView2 = findViewById(R.id.textview2);
        textView2.setText("用String传递int数据：" + stringIntValue);

        final TextView textView3 = findViewById(R.id.textview3);
        textView3.setText("接收包含大小写数字的String值传递：" + str_123_Value);

        final TextView textView4 = findViewById(R.id.textview4);
        textView4.setText("接收故意传递非boolean值给boolean变量：" + boolParseError);

        final TextView textview4_1 = findViewById(R.id.textview4_1);
        textview4_1.setText("用字符串传一个很大的值给short变量：" + shortParseError);

        final TextView textView5 = findViewById(R.id.textview5);
        textView5.setText("接收boolean值：boolValue=" + boolValue);

        final TextView textview6 = findViewById(R.id.textview6);
        textview6.setText("接收Long类型的值：longValue=" + longValue);

        final TextView textview7 = findViewById(R.id.textview7);
        textview7.setText("接收Char类型的值：" + charValue);

        final TextView textview8 = findViewById(R.id.textview8);
        textview8.setText("接收double类型的值(key与关键字同名情况)：" + doubleValue);

        final TextView textview9 = findViewById(R.id.textview9);
        textview9.setText("接收float类型的值：" + floatValue);

        final TextView textview10 = findViewById(R.id.textview10);
        textview10.setText("接收 SerializableObject 的值：" + serializableBean.hello);

        final TextView textview11 = findViewById(R.id.textview11);
        textview11.setText("接收 ParcelableObject 的值：" + parcelableBean.hello);

        if (button1 != null) {
            button1.setText("展示注解默认值");
            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(NavigatorTargetActivity.this, "提示:" + strFromAnnotation, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}