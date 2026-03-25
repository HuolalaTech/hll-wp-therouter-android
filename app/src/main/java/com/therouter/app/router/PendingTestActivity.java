package com.therouter.app.router;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.therouter.app.HomePathIndex;
import com.therouter.app.R;
import com.therouter.router.NavigatorKt;
import com.therouter.router.Route;

@Route(path = HomePathIndex.PENDING)
public class PendingTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pending);

        // 这一句可以主动释放所有挂起的导航动作
        // NavigatorKt.sendPendingNavigator();
    }
}
