package com.therouter.app.serviceprovider;

import com.therouter.demo.di.IJavaServiceProvider2;
import com.therouter.inject.ServiceProvider;

@ServiceProvider
public class JavaServiceProvider2 implements IJavaServiceProvider2 {
    @Override
    public String getString() {
        return "Hello Java Class";
    }
}
