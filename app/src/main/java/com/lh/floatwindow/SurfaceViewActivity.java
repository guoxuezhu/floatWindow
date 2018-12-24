package com.lh.floatwindow;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.app.AlertDialog;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


public class SurfaceViewActivity extends AppCompatActivity implements SurfaceHolder.Callback
        , MediaPlayer.OnPreparedListener {

    public static final float SHOW_SCALE = 16 * 1.0f / 9;

    private RelativeLayout mSurfaceLayout;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private MediaPlayer mMediaPlayer;

    //屏幕宽度
    private int mScreenWidth;
    //屏幕高度
    private int mScreenHeight;
    //记录现在的播放位置
    private int mCurrentPos;
    private boolean isLand;

    private DisplayMetrics displayMetrics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_surface_view);

        displayMetrics = new DisplayMetrics();
        this.getWindow().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;

        //后台异常终止，应当恢复原有位置播放
        if (savedInstanceState != null)
            mCurrentPos = savedInstanceState.getInt("currentPos", 0);

        initView();
    }

    private void initView() {

        mSurfaceLayout = (RelativeLayout) findViewById(R.id.layout_gesture);
        RelativeLayout.LayoutParams lp =
                (RelativeLayout.LayoutParams) mSurfaceLayout.getLayoutParams();
        lp.height = (int) (mScreenWidth * SHOW_SCALE);
        mSurfaceLayout.setLayoutParams(lp);

        mSurfaceView = (SurfaceView) findViewById(R.id.sv);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
    }

    private void resetSize() {

        float areaWH = 0.0f;
        int height;

        if (!isLand) {
            // 竖屏16:9
            height = (int) (mScreenWidth / SHOW_SCALE);
            areaWH = SHOW_SCALE;
        } else {
            //横屏按照手机屏幕宽高计算比例
            height = mScreenHeight;
            areaWH = mScreenWidth / mScreenHeight;
        }

        RelativeLayout.LayoutParams layoutParams =
                new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        mSurfaceLayout.setLayoutParams(layoutParams);

        int mediaWidth = mMediaPlayer.getVideoWidth();
        int mediaHeight = mMediaPlayer.getVideoHeight();


        float mediaWH = mediaWidth * 1.0f / mediaHeight;

        RelativeLayout.LayoutParams layoutParamsSV = null;


        if (areaWH > mediaWH) {
            //直接放会矮胖
            int svWidth = (int) (height * mediaWH);
            layoutParamsSV = new RelativeLayout.LayoutParams(svWidth, height);
            layoutParamsSV.addRule(RelativeLayout.CENTER_IN_PARENT);
            mSurfaceView.setLayoutParams(layoutParamsSV);
        }

        if (areaWH < mediaWH) {
            //直接放会瘦高。
            int svHeight = (int) (mScreenWidth / mediaWH);
            layoutParamsSV = new RelativeLayout.LayoutParams(mScreenWidth, svHeight);
            layoutParamsSV.addRule(RelativeLayout.CENTER_IN_PARENT);
            mSurfaceView.setLayoutParams(layoutParamsSV);
        }

    }

    private void initMediaPlayer() {

        if (mMediaPlayer != null) {//从Home 返回
            mMediaPlayer.setDisplay(mHolder);
            mMediaPlayer.start();
        } else {

            mMediaPlayer = new MediaPlayer();  //销毁返回重新初始化

            try {
                mMediaPlayer.setDataSource(this, Uri.parse("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"));
                mMediaPlayer.setLooping(true);
                mMediaPlayer.setDisplay(mHolder);
                mMediaPlayer.setOnPreparedListener(this);
                mMediaPlayer.prepareAsync();
                mMediaPlayer.setScreenOnWhilePlaying(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    //只有在点击Home键或者程序发生异常时才会执行此方法
    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt("currentPos", mCurrentPos);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //SV可见
        initMediaPlayer();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //SV状态变化
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            mCurrentPos = mMediaPlayer.getCurrentPosition();
        }
    }

    /**
     * 销毁掉MediaPlayer对象
     */
    private void releaseMP() {

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            System.gc();
        }

    }

    @Override
    public void onPrepared(MediaPlayer mp) {

        resetSize();
        if (mCurrentPos != 0) {
            mMediaPlayer.seekTo(mCurrentPos);
        }

        mMediaPlayer.start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        isLand = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;

        resetSize();
    }

    public void changeOrientation(View view) {
        if (Configuration.ORIENTATION_LANDSCAPE == this.getResources()
                .getConfiguration().orientation) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMP();

    }
}

