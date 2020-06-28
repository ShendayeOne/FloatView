package com.shenyang.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.shenyang.R;
import com.shenyang.otherviews.SyFloatView;

import android.util.Log;
import android.view.View;

import android.widget.Button;

public class MainActivity extends Activity {
    protected Button toButton,noButton,exitButton;
    protected Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initVariables();
        initViews(savedInstanceState);
        loadData();

        //造个悬浮球，并显示出来
        SyFloatView.getInstance(this).show();

        //这里可以根据需求，添加点击事件处理
        SyFloatView.getInstance(this).setListener(new SyFloatView.FloatingLayerListener() {
            @Override
            public void onClick() {
                //点击悬浮球事件
                Log.e("MainAc","这是 点击悬浮球 监听回调。");
            }

            @Override
            public void onClose() {
                //关闭悬浮球事件
                Log.e("MainAc","这是 关闭悬浮球 监听回调。");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //显示悬浮球：进入APP、登陆成功等场景下
        SyFloatView.getInstance(mContext).display();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //隐藏悬浮球：回到桌面等场景
        SyFloatView.getInstance(mContext).hide();
    }

    //关闭APP
    protected void exitApp(){
        //干掉悬浮球：可以在关闭APP、注销账号等场景下使用
        if(SyFloatView.isShowBall);SyFloatView.getInstance(mContext).close();
        System.exit(0);
    }



    //初始化
    protected void initVariables() {
        mContext = (Activity)this;
    }

    //初始化视图
    protected void initViews(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        toButton = (Button) findViewById(R.id.to_btn);
        noButton = (Button) findViewById(R.id.no_btn);
        exitButton = (Button) findViewById(R.id.exit_app_btn);

        //来消息了，显示消息红点
        toButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SyFloatView.getInstance(mContext).redDotVisible();
            }
        });

        //朕已阅，点击关掉消息红点
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SyFloatView.getInstance(mContext).redDotViewGone();
            }
        });

        //关闭APP
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitApp();
            }
        });
    }

    //数据处理
    protected void loadData() {
    }
}