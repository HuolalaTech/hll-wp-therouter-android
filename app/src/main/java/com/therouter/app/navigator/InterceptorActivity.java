package com.therouter.app.navigator;

import static com.therouter.app.HomePathIndex.INTERCEPTOR;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.therouter.app.R;
import com.therouter.router.NavigatorKt;
import com.therouter.router.Route;
import com.therouter.router.RouteItemKt;
import com.therouter.router.interceptor.NavigatorPathFixHandle;
import com.therouter.router.interceptor.PathReplaceInterceptor;
import com.therouter.router.interceptor.RouterReplaceInterceptor;

@Route(path = INTERCEPTOR)
public class InterceptorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.interceptor);
        NavigatorKt.addNavigatorPathFixHandle(new NavigatorPathFixHandle() {
            @Nullable
            @Override
            public String fix(@Nullable String path) {
                if (path != null) {
                    path = path.replace("http://", "https://");
                }
                return path;
            }
        });

        NavigatorKt.addPathReplaceInterceptor(new PathReplaceInterceptor() {
            @Nullable
            @Override
            public String replace(@Nullable String path) {
                // 在这里替换path
                return path;
            }
        });

        NavigatorKt.addRouterReplaceInterceptor(new RouterReplaceInterceptor() {
            @Nullable
            @Override
            public RouteItem replace(@Nullable RouteItem routeItem) {
                if (routeItem != null) {
                    Log.d("debug", "路由表内容：" + RouteItemKt.getUrlWithParams(routeItem));
                }
                return routeItem;
            }
        });

        Toast.makeText(this, "拦截器已插入", Toast.LENGTH_SHORT).show();
    }
}
