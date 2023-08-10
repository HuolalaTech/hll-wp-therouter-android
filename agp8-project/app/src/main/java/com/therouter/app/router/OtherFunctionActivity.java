package com.therouter.app.router;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.therouter.TheRouter;
import com.therouter.app.HomePathIndex;
import com.therouter.app.R;
import com.therouter.app.serviceprovider.IKotlinSerivce;
import com.therouter.router.Route;

@Route(path = HomePathIndex.DEMO_OTHER)
public class OtherFunctionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("TheRouter特有能力");
        setContentView(R.layout.other);
        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.build(HomePathIndex.DEMO_HISTORY).navigation();
            }
        });
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TheRouter.get(IKotlinSerivce.class, v.getContext(), "hello").hello();
            }
        });
    }
}
