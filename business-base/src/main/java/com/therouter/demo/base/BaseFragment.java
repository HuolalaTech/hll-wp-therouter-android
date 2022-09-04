package com.therouter.demo.base;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.therouter.TheRouter;

public class BaseFragment extends Fragment {
    protected Activity activity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        TheRouter.inject(this);
    }
}
