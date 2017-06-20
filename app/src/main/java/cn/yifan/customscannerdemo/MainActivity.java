package cn.yifan.customscannerdemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.zxing.Result;

import java.io.IOException;

import cn.yifan.scanner.Constans;
import cn.yifan.scanner.InactivityTimer;
import cn.yifan.scanner.impl.CaptureActivity;
import cn.yifan.scanner.CaptureActivityHandler;
import cn.yifan.scanner.camera.CameraManager;
import cn.yifan.scanner.widget.ViewfinderView;

/**
 * 扫描界面
 */
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback2, CaptureActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * 导航栏
     */
    Toolbar mTitleBar;

    /**
     * 预览控件
     */
    private SurfaceView mSurfaceView;

    /**
     * 判断是否已经初始化
     */
    private boolean hasSurface;

    /**
     * Camera Api控制
     */
    private CameraManager mCameraManager;

    /**
     * 扫描框控件
     */
    private ViewfinderView mViewfinderView;

    /**
     * handler回调
     */
    private CaptureActivityHandler mHandler;

    /**
     * 保存用于展示扫描的结果
     */
    private Result mSavedResultToShow;

    /**
     * 计时器，电池模式下超时自动关闭
     */
    private InactivityTimer mInactivityTimer;

    /**
     * 屏幕方向监听器
     */
    private OrientationDetector mOrientationDetector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化控件对象
        mTitleBar = (Toolbar) findViewById(R.id.titlebar);
        mSurfaceView = (SurfaceView) findViewById(R.id.sv_capture);
        mViewfinderView = (ViewfinderView) findViewById(R.id.vfv_capture);

        setSupportActionBar(mTitleBar);
        mInactivityTimer = new InactivityTimer(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            }
        }

        mOrientationDetector = new OrientationDetector(this);
        mOrientationDetector.setLastOrientation(getWindowManager().getDefaultDisplay().getRotation());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
        if (PreferenceManager.getDefaultSharedPreferences(getApplication()).getBoolean(Constans.KEY_DISABLE_AUTO_ORIENTATION, true)) {
            setRequestedOrientation(getRequestedOrientation());
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR); // 旋转
            mOrientationDetector.enable(); //启用监听
        }
        mInactivityTimer.onResume();
        if (null == mCameraManager) {
            mCameraManager = new CameraManager(getApplication());
            mViewfinderView.setCameraManager(mCameraManager);
        }
        //设置竖屏方向
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        //绑定Surface回调
        SurfaceHolder holder = mSurfaceView.getHolder();
        if (hasSurface) {
            //已经初始化，直接使用
            initCamera(holder);
        } else {
            holder.addCallback(this);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause: ");
        if (mHandler != null) {
            mHandler.quitSynchronously();
            mHandler = null;
        }
        mInactivityTimer.onPause();
        if (null != mCameraManager) {
            mCameraManager.closeDriver();
            mCameraManager = null;
        }
        if (hasSurface && null != mSurfaceView) {
            mSurfaceView.getHolder().removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (null != mSurfaceView) {
            mSurfaceView.getHolder().removeCallback(this);
        }
        hasSurface = false;
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mInactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public void surfaceRedrawNeeded(SurfaceHolder holder) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //do nothing
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed: ");
        hasSurface = false;
    }

    /**
     * 初始化相机
     */
    private void initCamera(SurfaceHolder holder) {
        Log.i(TAG, "initCamera: ");
        if (null == holder) {
            throw new NullPointerException("holder is null!!!");
        }
        if (mCameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            mCameraManager.openDriver(mSurfaceView.getHolder());
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (mHandler == null) {
                mHandler = new CaptureActivityHandler(this, null, null, null, mCameraManager);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
        } catch (RuntimeException e) {
            Log.w(TAG, "Unexpected error initializing camera", e);
        }
    }


    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (mHandler == null) {
            mSavedResultToShow = result;
        } else {
            if (result != null) {
                mSavedResultToShow = result;
            }
            if (mSavedResultToShow != null) {
                Message message = Message.obtain(mHandler, R.id.decode_succeeded, mSavedResultToShow);
                mHandler.sendMessage(message);
            }
            mSavedResultToShow = null;
        }
    }

    @Override
    public void drawViewfinder() {
        if (null != mViewfinderView) {
            mViewfinderView.drawViewfinder();
        }
    }

    @Override
    public void destory() {

    }


    @Override
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        mInactivityTimer.onActivity();

        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("扫描结果").setMessage(rawResult.getText())
                .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        restartPreviewAfterDelay(10L);
                    }
                }).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

    }

    public void restartPreviewAfterDelay(long delayMS) {
        Log.i(TAG, "restartPreviewAfterDelay: ");
        if (mHandler != null) {
            mHandler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
        resetStatusView();
    }

    private void resetStatusView() {
    }

    @Override
    public CameraManager getCameraManager() {
        return mCameraManager;
    }

    @Override
    public ViewfinderView getViewfinderView() {
        return mViewfinderView;
    }

    @Override
    public Handler getHandler() {
        return mHandler;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    /**
     * 屏幕方向变动监听器
     */
    private class OrientationDetector extends OrientationEventListener {

        private int lastOrientation = -1;

        OrientationDetector(Context context) {
            super(context);
        }

        void setLastOrientation(int rotation) {
            switch (rotation) {
                case Surface.ROTATION_90:
                    lastOrientation = 270;
                    break;
                case Surface.ROTATION_270:
                    lastOrientation = 90;
                    break;
                default:
                    lastOrientation = -1;
            }
        }

        @Override
        public void onOrientationChanged(int orientation) {
            Log.d(TAG, "orientation:" + orientation);
            if (orientation > 45 && orientation < 135) {
                orientation = 90;
            } else if (orientation > 225 && orientation < 315) {
                orientation = 270;
            } else {
                orientation = -1;
            }
            if ((orientation == 90 && lastOrientation == 270) || (orientation == 270 && lastOrientation == 90)) {
                Log.i(TAG, "orientation:" + orientation + "lastOrientation:" + lastOrientation);
                Intent intent = getIntent();
                finish();
                startActivity(intent);
                lastOrientation = orientation;
                Log.i(TAG, "SUCCESS");
            }
        }
    }
}
