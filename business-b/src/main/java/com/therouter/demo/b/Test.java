package com.therouter.demo.b;

import com.therouter.demo.di.IPushService;
import com.therouter.demo.di.IUserService;
import com.therouter.inject.ServiceProvider;

public class Test {

    /**
     * 方法名不限定，任意名字都行，方法可以写在任何地方
     * 返回值必须是服务接口名，如果是实现了服务的子类，需要加上returnType限定
     * 方法必须加上 public static 修饰，否则编译期就会报错
     */
    @ServiceProvider
    public static IUserService test() {
        return new IUserService() {
            @Override
            public String getUserInfo() {
                return "这是用户信息";
            }
        };
    }

    @ServiceProvider(path =  "/push/demo")
    public static IPushService demo() {
        return new IPushService() {

            @Override
            public void onPush(String jsonStr) {

            }
        };
    }

    @ServiceProvider(path =  "/push/str")
    public static IPushService demo(String str) {
        return new IPushService() {

            @Override
            public void onPush(String jsonStr) {

            }
        };
    }
}
