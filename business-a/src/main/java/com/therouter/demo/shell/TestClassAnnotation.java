package com.therouter.demo.shell;

import com.therouter.demo.di.ITest0;
import com.therouter.demo.di.ITestClassAnnotation;
import com.therouter.inject.ServiceProvider;

@ServiceProvider(returnType = ITestClassAnnotation.class)
public class TestClassAnnotation implements ITestClassAnnotation, ITest0 {
    @Override
    public String getMessage() {
        return "test";
    }
}
