package com.therouter.app.action;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.therouter.app.HomePathIndex;
import com.therouter.router.Navigator;
import com.therouter.router.action.interceptor.ActionInterceptor;

@com.therouter.router.action.ActionInterceptor(actionName = HomePathIndex.ACTION2)
public class TestActionInterceptor extends ActionInterceptor {

    @Override
    public boolean handle(@NonNull Context context, @NonNull Navigator navigator) {
        Toast.makeText(context, HomePathIndex.ACTION2, Toast.LENGTH_SHORT).show();
        return super.handle(context, navigator);
    }
}
