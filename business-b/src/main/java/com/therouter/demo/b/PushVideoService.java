package com.therouter.demo.b;

import com.therouter.demo.di.IPushService;
import com.therouter.inject.ServiceProvider;

@ServiceProvider(path = "/push/video")
public class PushVideoService implements IPushService {
    @Override
    public void onPush(String jsonStr) {

    }
}
