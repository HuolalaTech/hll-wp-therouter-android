package com.therouter.demo.shell;

import com.therouter.demo.di.ITestClassAnnotation2;
import com.therouter.inject.ServiceProvider;

/**
 * 如果是只有一个父接口，则 @ServiceProvider 可以不声明返回值信息
 */
@ServiceProvider
public class TestClassAnnotation2 implements ITestClassAnnotation2 {
    @Override
    public String getTestMessage() {
        return "TestClassAnnotation2";
    }
}
