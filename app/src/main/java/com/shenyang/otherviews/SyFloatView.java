package com.shenyang.otherviews;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.shenyang.R;

import java.util.Timer;
import java.util.TimerTask;

public class SyFloatView {

    private static SyFloatView sFloatingLayer;
    private static final String TAG = "SyFloatView";
    public static boolean isShowBall = false;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams,halfPop_mLayoutParams;
    private Context mContext;
    private final int TOUCH_TIME_THRESHOLD = 150;
    private View mPopView;
    private float stickRightWidth;
    private AnimationTimerTask mAnimationTask;
    private Timer mAnimationTimer;
    private GetTokenRunnable mGetTokenRunnable;
    private long mLastTouchDownTime;

    private FloatingLayerListener mListener;

    private int mWidth,mHeight;
    private float mPrevX, xInScreen, xDownInScreen;
    private float mPrevY, yInScreen, yDownInScreen;
    private int mAnimationPeriodTime = 16;
    private static ImageView barRed, ballImage;

    private Handler mHandler = new Handler();

    private long lastClickTime;

    public static SyFloatView getInstance(Context context) {
        if (null == sFloatingLayer) {
            synchronized (SyFloatView.class) {
                if (null == sFloatingLayer) {
                    sFloatingLayer = new SyFloatView(context);
                }
            }
        }
        return sFloatingLayer;
    }

    private SyFloatView(Context context) {
        this.mContext = context;

        initView();
        initWindowManager();
        initLayoutParams();
        initDrag();
    }

    private void initView() {
        LayoutInflater layoutInflater = (LayoutInflater) ((Activity)mContext).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopView = layoutInflater.inflate(R.layout.sy_floating_view, null);

        //这里可以自定义图片，消息红点
        barRed = (ImageView) mPopView.findViewById(R.id.barRed);
        ballImage = (ImageView) mPopView.findViewById(R.id.pop);
    }

    private void initWindowManager() {
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    }

    private void initLayoutParams() {
        mWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        mHeight = mContext.getResources().getDisplayMetrics().heightPixels;

        mLayoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        mLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;

        //初始显示的位置
        mLayoutParams.x = 0;
        mLayoutParams.y = mContext.getResources().getDisplayMetrics().heightPixels / 3 * 2;
    }

    private void initDrag() {
        mPopView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        mLastTouchDownTime = System.currentTimeMillis();
                        handler.removeMessages(1);
                        xInScreen = mPrevX = motionEvent.getRawX();
                        yInScreen = mPrevY = motionEvent.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = motionEvent.getRawX() - mPrevX;
                        float deltaY = motionEvent.getRawY() - mPrevY;
                        mLayoutParams.x += deltaX;
                        mLayoutParams.y += deltaY;
                        xDownInScreen = mPrevX = motionEvent.getRawX();
                        yDownInScreen = mPrevY = motionEvent.getRawY();

                        if (mLayoutParams.x < 0) mLayoutParams.x = 0;
                        if (mLayoutParams.x > mWidth - mPopView.getWidth())
                            mLayoutParams.x = mWidth - mPopView.getWidth();
                        if (mLayoutParams.y < 0) mLayoutParams.y = 0;
                        if (mLayoutParams.y > mHeight - mPopView.getHeight() * 2)
                            mLayoutParams.y = mHeight - mPopView.getHeight() * 2;

                        try {
                            mWindowManager.updateViewLayout(mPopView, mLayoutParams);
                        } catch (Exception e) {
                            Log.d(TAG, e.toString());
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        if (isOnClickEvent()) {
                            //添加点击事件
                            if (isFastDoubleClick()) {
                                //防止连续点击，如果连续点击这里什么也不做
                            } else {
                                Toast.makeText(mContext, "你点击了悬浮球", Toast.LENGTH_SHORT).show();
                                //点击就让消息红点消失
                                redDotViewGone();
                            };
                            if (mListener != null) {
                                mListener.onClick();
                            }
                        }

                        if (mLayoutParams.x == stickRightWidth) {
                            mLayoutParams.x = mLayoutParams.x - mPopView.getWidth() / 2;
                            mWindowManager.updateViewLayout(mPopView, mLayoutParams);
                            sendMsgToHidePop();
                            return false;
                        }
                        mAnimationTimer = new Timer();
                        mAnimationTask = new AnimationTimerTask();
                        mAnimationTimer.schedule(mAnimationTask, 0, mAnimationPeriodTime);
                        sendMsgToHidePop();
                        break;
                }

                return false;
            }
        });
    }

    //防止连续点击
    private boolean isFastDoubleClick() {
        long time = System.currentTimeMillis();
        if (time - lastClickTime < 500) {
            return true;
        }
        lastClickTime = time;
        return false;
    }

    protected boolean isOnClickEvent() {
        return System.currentTimeMillis() - mLastTouchDownTime < TOUCH_TIME_THRESHOLD;
    }

    /**
     * 创建悬浮球并显示出来
     */
    public void show() {
        if (!isShowBall) {
            mGetTokenRunnable = new GetTokenRunnable(((Activity)mContext));
            mHandler.postDelayed(mGetTokenRunnable, 500);
            isShowBall = true;
        }
    }

    /**
     * 杀掉悬浮球
     */
    public void close() {
        try {
            if (isShowBall) {
                mWindowManager.removeViewImmediate(mPopView);
                isShowBall = false;
                if (null != mListener) {
                    mListener.onClose();
                }
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }

    }

    /**
     * 隐藏悬浮球
     */
    public void hide() {
        if (mPopView != null) mPopView.setVisibility(View.INVISIBLE);
    }

    /**
     * 显示悬浮球
     */
    public void display() {
        if (mPopView != null) mPopView.setVisibility(View.VISIBLE);
    }

    /**
     * 显示悬浮球的红点
     */
    public void redDotVisible() {
        if (barRed != null) barRed.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏悬浮球的红点
     */
    public void redDotViewGone() {
        if (barRed != null) barRed.setVisibility(View.GONE);
    }


    public SyFloatView setListener(FloatingLayerListener listener) {
        if (null != sFloatingLayer) this.mListener = listener;
        return sFloatingLayer;
    }

    /**
     * 监听接口
     */
    public interface FloatingLayerListener {
        void onClick();
        void onClose();
    }


    /**
     * handler处理：隐藏半球
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    hidePop();
                    break;
                default:
                    break;
            }
        }
    };
    private static boolean isNearLeft = true;
    private class AnimationTimerTask extends TimerTask {

        int mStepX;
        int mDestX;

        public AnimationTimerTask() {
            if (mLayoutParams.x > mWidth / 2) {
                isNearLeft = false;
                mDestX = mWidth - mPopView.getWidth();
                mStepX = (mWidth - mLayoutParams.x) / 10;
            } else {
                isNearLeft = true;
                mDestX = 0;
                mStepX = -((mLayoutParams.x) / 10);
            }
        }

        @Override
        public void run() {
            if (Math.abs(mDestX - mLayoutParams.x) <= Math.abs(mStepX)) {
                mLayoutParams.x = mDestX;
            } else {
                mLayoutParams.x += mStepX;
            }
            try {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mWindowManager.updateViewLayout(mPopView, mLayoutParams);
                    }
                });
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
            if (mLayoutParams.x == mDestX) {
                mAnimationTask.cancel();
                mAnimationTimer.cancel();
            }
        }


    }

    public void sendMsgToHidePop() {
        Message msg = new Message();
        msg.what = 1;
        handler.sendMessageDelayed(msg, 2500);
    }

    /**
     * 隐藏悬浮球一半，贴在边缘
     */
    private void hidePop() {
        halfPop_mLayoutParams = mLayoutParams;
        if (isNearLeft) {
            halfPop_mLayoutParams.x = -(mPopView.getWidth() / 2);
        } else {
            halfPop_mLayoutParams.x = mLayoutParams.x + (mPopView.getWidth() / 2);
            stickRightWidth = halfPop_mLayoutParams.x;
        }

        try {
            mWindowManager.updateViewLayout(mPopView, halfPop_mLayoutParams);
        } catch (Exception e) {
            Log.d(TAG, "mWindowManager.updateViewLayout 出错，e :" + e.toString());
        }
    }

    private class GetTokenRunnable implements Runnable {
        int count = 0;
        private Activity mActivity;

        public GetTokenRunnable(Activity activity) {
            this.mActivity = activity;
        }

        @Override
        public void run() {

            if (null == mActivity) return;
            IBinder token = null;
            try {
                token = mActivity.getWindow().getDecorView().getWindowToken();
            } catch (Exception e) {
            }

            if (null != token) {

                try {
                    mLayoutParams.token = token;
                    if (mWindowManager != null && mPopView != null && mLayoutParams != null)
                        mWindowManager.addView(mPopView, mLayoutParams);
                    mActivity = null;
                    return;
                } catch (Exception e) {
                }
            }

            count++;
            mLayoutParams.token = null;
            if (count < 10 && null != mLayoutParams) {
                mHandler.postDelayed(mGetTokenRunnable, 500);
            }

        }
    }

}

