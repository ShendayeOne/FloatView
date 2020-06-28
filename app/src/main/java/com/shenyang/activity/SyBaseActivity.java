package com.shenyang.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;

abstract class SyBaseActivity  extends Activity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        initVariables();
        initViews(savedInstanceState);
        loadData();
    }

    //初始化变量
    protected abstract void initVariables();

    //初始化view
    protected abstract void initViews(Bundle savedInstanceState);

    //数据处理
    protected abstract void loadData();
}
