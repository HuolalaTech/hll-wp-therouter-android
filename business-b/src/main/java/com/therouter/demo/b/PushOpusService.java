package com.therouter.demo.b;

import com.therouter.demo.di.IPushService;
import com.therouter.inject.ServiceProvider;

@ServiceProvider(path = "/push/opus")
public class PushOpusService implements IPushService {
    @Override
    public void onPush(String jsonStr) {
    }
}
