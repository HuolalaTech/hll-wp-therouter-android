package com.therouter.demo.shell;

import com.therouter.demo.di.ITest;
import com.therouter.inject.ServiceProvider;

public class Test {

    /**
     * 随便找个地方写一个
     *
     * @return
     */
    @ServiceProvider
    public static ITest create() {
        return new ITest() {
            @Override
            public String getString() {
                return null;
            }
        };
    }

}
