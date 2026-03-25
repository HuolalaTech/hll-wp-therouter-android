package com.therouter.app.serviceprovider;

import com.therouter.demo.di.IJavaServiceProvider;
import com.therouter.inject.ServiceProvider;

public class JavaServiceProvider {
    @ServiceProvider
    public static IJavaServiceProvider create() {
        return new IJavaServiceProvider() {
            @Override
            public String getString() {
                return "Hello Java Method";
            }
        };
    }
}
