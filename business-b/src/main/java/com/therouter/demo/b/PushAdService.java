package com.therouter.demo.b;

import com.therouter.demo.di.IPushService;
import com.therouter.inject.ServiceProvider;

@ServiceProvider(path = "/push/ad")
public class PushAdService implements IPushService {
    @Override
    public void onPush(String jsonStr) {

    }
}
