package com.therouter.demo.base;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.therouter.TheRouter;

public class BaseActivity extends AppCompatActivity {
    protected BaseActivity activity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        activity = this;
        super.onCreate(savedInstanceState);
        TheRouter.inject(this);
    }
}
