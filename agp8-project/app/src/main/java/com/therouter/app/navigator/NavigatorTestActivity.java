package com.therouter.app.navigator;

import static com.therouter.app.KotlinPathIndex.Test.HOME2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.therouter.TheRouter;
import com.therouter.app.HomePathIndex;
import com.therouter.app.KotlinPathIndex;
import com.therouter.app.R;
import com.therouter.app.router.InternalBeanTest;
import com.therouter.router.Navigator;
import com.therouter.router.Route;
import com.therouter.router.interceptor.NavigationCallback;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@Route(path = HomePathIndex.DEMO_NAVIGATOR)
public class NavigatorTestActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigator);
        setTitle("导航测试用例");
        // @Autowired 注入，这一行应该写在BaseActivity中更好
        TheRouter.inject(this);

        InternalBeanTest.RowBean bean = new InternalBeanTest.RowBean();
        bean.setHello("helloField");

        ArrayList<ArrayList<String>> stringChildClassFields = new ArrayList<>();
        ArrayList<String> item = new ArrayList<>();
        item.add("stringChildClassFields");
        stringChildClassFields.add(item);

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.HOME)
                        .withInt("intValue", 12345678) // 测试传 int 值
                        .withString("stringIntValue", "12345678")// 测试用 string 传 int 值
                        .withString("str_123_Value", "测试传中文字符串")// 测试 string
                        .withString("boolParseError", "非boolean值") // 测试用 boolean 解析字符串的情况
                        .withString("shortParseError", "12345678") // 测试用 short 解析超长数字的情况
                        .withBoolean("boolValue", true) // 测试 boolean
                        .withLong("longValue", 123456789012345L)  // 测试 long
                        .withChar("charValue", 'c')  // 测试 char
                        .withDouble("double", 3.14159265358972)// 测试double，key与关键字冲突
                        .withFloat("floatValue", 3.14159265358972F)// 测试float，自动四舍五入
                        .withSerializable("SerializableObject", bean)
                        .withParcelable("ParcelableObject", bean)
                        .withString("stringChildClassField", "数据在子类解析")// 测试 string
                        .withSerializable("stringChildClassFields", stringChildClassFields) // 嵌套的泛型参数
                        .withObject("runnable", new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(v.getContext(), "来自 withObject 的 toast", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .navigation();
            }
        });
        findViewById(R.id.button1_0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = TheRouter.build(KotlinPathIndex.Test.FRAGMENT_TEST2)
                        .withInt("intValue", 12345678) // 测试传 int 值
                        .withString("stringIntValue", "12345678")// 测试用 string 传 int 值
                        .withString("str_123_Value", "测试传中文字符串")// 测试 string
                        .withString("boolParseError", "非boolean值") // 测试用 boolean 解析字符串的情况
                        .withString("shortParseError", "12345678") // 测试用 short 解析超长数字的情况
                        .withBoolean("boolValue", true) // 测试 boolean
                        .withLong("longValue", 123456789012345L)  // 测试 long
                        .withChar("charValue", 'c')  // 测试 char
                        .withDouble("double", 3.14159265358972)// 测试double，key与关键字冲突
                        .withFloat("floatValue", 3.14159265358972F)// 测试float，自动四舍五入
                        .withSerializable("SerializableObject", bean)
                        .withParcelable("ParcelableObject", bean)
                        .withString("stringChildClassField", "数据在子类解析")// 测试 string
                        .withSerializable("stringChildClassFields", stringChildClassFields) // 嵌套的泛型参数
                        .createFragment();
                TheRouter.build(KotlinPathIndex.Test.FRAGMENT_HOST).withObject("fragment", fragment).navigation();
            }
        });
        findViewById(R.id.button1_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String urlWithParams = TheRouter.build(HomePathIndex.HOME)
                        .withInt("intValue", 12345678)
                        .withString("stringIntValue", "12345678")
                        .withString("str_123_Value", "测试传中文字符串")
                        .withString("boolParseError", "非boolean值")
                        .withString("shortParseError", "12345678")
                        .withBoolean("boolValue", true)
                        .withLong("longValue", 123456789012345L)
                        .withChar("charValue", 'c')
                        .withDouble("double", 3.14159265358972)
                        .withFloat("floatValue", 3.14159265358972F)
                        .withSerializable("SerializableObject", bean)
                        .withParcelable("ParcelableObject", bean)
                        .withString("stringChildClassField", "数据在子类解析")// 测试 string
                        .withSerializable("stringChildClassFields", stringChildClassFields) // 嵌套的泛型参数
                        .withObject("runnable", new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(v.getContext(), "来自 withObject 的 toast", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .getUrlWithParams();
                Toast.makeText(v.getContext(), urlWithParams, Toast.LENGTH_LONG).show();
            }
        });
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HOME2)
                        .withInt("intValue", 12345678) // 测试传 int 值
                        .withString("stringIntValue", "9876543")// 测试用 string 传 int 值
                        .withString("str_123_Value", "使用另一个path跳转过来")// 测试 string
                        .withString("boolParseError", "非boolean值") // 测试用 boolean 解析字符串的情况
                        .withString("shortParseError", "12345678") // 测试用 short 解析超长数字的情况
                        .withBoolean("boolValue", true) // 测试 boolean
                        .withLong("longValue", 123456789012345L)  // 测试 long
                        .withChar("charValue", 'c')  // 测试 char
                        .withDouble("double", 3.14159265358972)// 测试double，key与关键字冲突
                        .withFloat("floatValue", 3.14159265358972F)// 测试float，自动四舍五入
                        .withSerializable("SerializableObject", bean)
                        .withParcelable("ParcelableObject", bean)
                        .withString("stringChildClassField", "数据在子类解析")// 测试 string
                        .withSerializable("stringChildClassFields", stringChildClassFields) // 嵌套的泛型参数
                        .withObject("runnable", new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(v.getContext(), "来自 withObject 的 toast", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .navigation(v.getContext(), new NavigationCallback() {
                            @Override
                            public void onFound(@NotNull Navigator navigator) {
                                super.onFound(navigator);
                                Toast.makeText(v.getContext(), "找到页面，即将打开" + navigator.getUrl(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onLost(@NotNull Navigator navigator) {
                                super.onLost(navigator);
                                Toast.makeText(v.getContext(), "丢失页面" + navigator.getUrl(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onArrival(@NotNull Navigator navigator) {
                                super.onArrival(navigator);
                                Toast.makeText(v.getContext(), "已打开" + navigator.getUrl(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onActivityCreated(@NonNull Navigator navigator, @NonNull Activity activity) {
                                super.onActivityCreated(navigator, activity);
                                Toast.makeText(v.getContext(), "落地页已创建", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        findViewById(R.id.button2_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build("https://kymjs.com/404").navigation(v.getContext(), new NavigationCallback() {
                    @Override
                    public void onFound(@NotNull Navigator navigator) {
                        super.onFound(navigator);
                        Toast.makeText(v.getContext(), "找到页面" + navigator.getUrl(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLost(@NotNull Navigator navigator) {
                        super.onLost(navigator);
                        Toast.makeText(v.getContext(), "丢失页面" + navigator.getUrl(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onArrival(@NotNull Navigator navigator) {
                        super.onArrival(navigator);
                        Toast.makeText(v.getContext(), "已打开" + navigator.getUrl(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        findViewById(R.id.button2_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.OBJECT_TEST2).navigation(v.getContext(), new NavigationCallback() {
                    @Override
                    public void onFound(@NotNull Navigator navigator) {
                        super.onFound(navigator);
                        Toast.makeText(v.getContext(), "找到页面，即将打开" + navigator.getUrl(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLost(@NotNull Navigator navigator) {
                        super.onLost(navigator);
                        Toast.makeText(v.getContext(), "丢失页面" + navigator.getUrl(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onArrival(@NotNull Navigator navigator) {
                        super.onArrival(navigator);
                        Toast.makeText(v.getContext(), "已打开" + navigator.getUrl(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(HomePathIndex.HOME)
                        .append("?")
                        .append("intValue=12345678")
                        .append("&")
                        .append("stringIntValue=12345678")
                        .append("&")
                        .append("str_123_Value=测试传中文字符串")
                        .append("&")
                        .append("boolParseError=非boolean值")
                        .append("&")
                        .append("shortParseError=12345678")
                        .append("&")
                        .append("boolValue=true")
                        .append("&")
                        .append("longValue=123456789012345")
                        .append("&")
                        .append("charValue=c")
                        .append("&")
                        .append("double=3.14159265358972")
                        .append("&")
                        .append("floatValue=3.14159265358972");
                TheRouter.build(stringBuilder.toString())
                        .withSerializable("SerializableObject", bean)
                        .withParcelable("ParcelableObject", bean)
                        .withString("stringChildClassField", "数据在子类解析")// 测试 string
                        .withSerializable("stringChildClassFields", stringChildClassFields) // 嵌套的泛型参数
                        .navigation();
            }
        });
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.KOTLIN).navigation();
            }
        });
        findViewById(R.id.button5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.KOTLIN2).navigation();
            }
        });
        findViewById(R.id.button6).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                TheRouter.build(HomePathIndex.OBJECT_TEST).withObject("callback", new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(v.getContext(), "来自 NavigatorTestActivity 的 toast", Toast.LENGTH_SHORT).show();
                    }
                }).navigation();
            }
        });
        findViewById(R.id.button7).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.INTERCEPTOR).navigation();
            }
        });
        findViewById(R.id.button8).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. 在使用第三方SDK的场景下，如果不方便获取落地页path，可以兼容使用Intent方式跳转

                // 2. 路由也可以动态添加路由表，可以为第三方SDK添加一个页面path，推荐使用这种方式
                // 然后使用path跳转，这样子支持线上通过动态修改路由表，来修改落地页达到动态化的目的
                Intent intent = new Intent(v.getContext(), NavigatorTargetActivity.class);
                intent.putExtra("intValue", 12345678) // 测试传 int 值
                        .putExtra("stringIntValue", "9876543") // 测试用 string 传 int 值
                        .putExtra("str_123_Value", "测试传中文字符串");
                TheRouter.build(intent)
                        .withString("str_123_Value", "测试用路由覆盖intent里面的值") //测试用路由覆盖intent里面的值
                        .withBoolean("boolValue", true) // 测试 boolean
                        .withLong("longValue", 123456789012345L)  // 测试 long
                        .withChar("charValue", 'c')  // 测试 char
                        .withDouble("double", 3.14159265358972)// 测试double，key与关键字冲突
                        .withFloat("floatValue", 3.14159265358972F)// 测试float，自动四舍五入
                        .navigation();
            }
        });
    }
}
