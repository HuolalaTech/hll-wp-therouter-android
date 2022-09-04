package com.therouter.demo.b;

import android.content.Context;

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
import com.therouter.inject.ServiceProvider;

/**
 * Created by ZhangTao on 17/8/15.
 * <p>
 * 测试对象生成与缓存
 */
public class Test3 implements ITest1, ITest2, ITest3, ITest4, ITest5, ITest6, ITest7, ITest8, ITest0, ITest11 {

    @ServiceProvider
    public static Test3 createTestx() {
        return new Test3();
    }

    @ServiceProvider
    public static Test test() {
        return new Test();
    }

    @ServiceProvider
    public static ITest1 createTest() {
        return new Test3();
    }

    @ServiceProvider
    public static ITest2 createTest1() {
        return new Test3();
    }

    @ServiceProvider
    public static ITest1 createTest1(final Context context) {
        return new Test3() {
            @Override
            public String toString() {
                return "一个参数：" + context.getClass().getName();
            }
        };
    }

    @ServiceProvider
    public static ITest1 createTest1(final Context context, final int str) {
        return new Test3() {
            @Override
            public String toString() {
                return "两个参数：" + context.getClass().getName() + "----字符串：" + str;
            }
        };
    }

    @ServiceProvider
    public static ITest2 createTest2() {
        return new Test3();
    }

    @ServiceProvider
    public static ITest3 createTest3() {
        return new Test3();
    }

    @ServiceProvider
    public static ITest4 createTest4() {
        return new Test3();
    }

    @ServiceProvider
    public static ITest5 createTest5() {
        return new Test3();
    }

    @ServiceProvider
    public static ITest6 createTest6() {
        return new Test3();
    }

    @ServiceProvider
    public static ITest7 createTest7() {
        return new Test3();
    }

    @ServiceProvider
    public static ITest8 createTest8() {
        return new Test3();
    }

    @ServiceProvider
    public static ITest0 createTest0() {
        return new Test3();
    }

    @ServiceProvider
    public static ITest10 createTest10() {
        return new ITest10() {
            @Override
            public String getMessage() {
                return null;
            }

            @Override
            public String getString() {
                return null;
            }
        };
    }

    @ServiceProvider
    public static ITest11 createTest11() {
        return new Test3();
    }

    @Override
    public String toString() {
        return "" + hashCode();
    }

    @Override
    public String getMessage() {
        return null;
    }
}
