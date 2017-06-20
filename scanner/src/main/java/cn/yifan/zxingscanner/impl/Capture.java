package cn.yifan.zxingscanner.impl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.zxing.Result;

import java.io.IOException;

import cn.yifan.zxingscanner.CaptureActivityHandler;
import cn.yifan.zxingscanner.Constans;
import cn.yifan.zxingscanner.InactivityTimer;
import cn.yifan.zxingscanner.R;
import cn.yifan.zxingscanner.camera.CameraManager;
import cn.yifan.zxingscanner.widget.ViewfinderView;

/**
 * 扫描api及生命周期管理工具
 *
 * Created by wuyifan on 2017/6/19.
 */

public class Capture implements CaptureActivity {


    private static final String TAG = Capture.class.getSimpleName();


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
     * 扫码界面操作对象
     */
    private Activity mActivity;

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

    /**
     * 扫描监听器
     */
    private OnScannerListener mOnScannerListener;

    public Capture.OnScannerListener getOnScannerListener() {
        return mOnScannerListener;
    }

    public void setOnScannerListener(Capture.OnScannerListener onScannerListener) {
        mOnScannerListener = onScannerListener;
    }

    /**
     * 构造函数
     *
     * @param activity       用于绑定,实现生命周期管理的{@link Activity}
     * @param surfaceView    展示CameraPreview的{@link SurfaceView} 控件
     * @param viewfinderView 扫码框控件{@link ViewfinderView}
     */
    public Capture(Activity activity, SurfaceView surfaceView, ViewfinderView viewfinderView) {
        //初始化，绑定控件对象
        this.mActivity = activity;
        this.mSurfaceView = surfaceView;
        this.mViewfinderView = viewfinderView;

        mInactivityTimer = new InactivityTimer(activity);

        mOrientationDetector = new OrientationDetector(mActivity);
        mOrientationDetector.setLastOrientation(mActivity.getWindowManager().getDefaultDisplay().getRotation());
    }

    /**
     * 被遮挡或退到后台后，返回扫码主界面
     *
     * @param callback {@link SurfaceView}的{@link android.view.SurfaceHolder.Callback}回调
     */
    public void resume(SurfaceHolder.Callback callback) {
        if (PreferenceManager.getDefaultSharedPreferences(mActivity)
                .getBoolean(Constans.KEY_DISABLE_AUTO_ORIENTATION, true)) {
            mActivity.setRequestedOrientation(
                    mActivity.getRequestedOrientation());
        } else {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR); // 旋转
            mOrientationDetector.enable(); //启用监听
        }
        mInactivityTimer.onResume();
        if (null == mCameraManager) {
            mCameraManager = new CameraManager(mActivity.getApplication());
            mViewfinderView.setCameraManager(mCameraManager);
        }
        //绑定Surface回调
        SurfaceHolder holder = mSurfaceView.getHolder();
        if (hasSurface) {
            //已经初始化，直接使用
            initCamera(holder);
        } else {
            holder.addCallback(callback);
        }
    }

    /**
     * 扫码界面被遮挡，暂定扫码识别功能，防止误扫
     *
     * 暂停倒计时、CameraPreview等
     *
     * @param callback {@link SurfaceHolder.Callback}
     */
    public void pause(SurfaceHolder.Callback callback) {
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
            mSurfaceView.getHolder().removeCallback(callback);
        }
    }

    /**
     * app退到后台，或该activity不可见
     *
     * @param callback {@link SurfaceHolder.Callback}
     */
    public void stop(SurfaceHolder.Callback callback) {
        if (null != mSurfaceView) {
            mSurfaceView.getHolder().removeCallback(callback);
        }
        hasSurface = false;
    }

    /**
     * 绑定{@link android.view.SurfaceHolder.Callback#surfaceCreated(SurfaceHolder)}函数
     *
     * @param holder {@link SurfaceHolder}
     */
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    /**
     * 绑定{@link android.view.SurfaceHolder.Callback#surfaceDestroyed(SurfaceHolder)}函数
     *
     * @param holder {@link SurfaceHolder}
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed: ");
        hasSurface = false;
    }

    /**
     * 生命周期结束，在对应{@link Activity#onDestroy()}函数中调用
     */
    public void destory() {
        mInactivityTimer.shutdown();
    }

    @Override
    public void setResult(int resultOk, Intent obj) {
        mActivity.setResult(resultOk, obj);
    }

    /**
     * 初始化相机
     *
     * @param holder
     */
    private void initCamera(SurfaceHolder holder) {
        hasSurface = true;
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
    public void startActivity(Intent intent) {
        mActivity.startActivity(intent);
    }

    @Override
    public PackageManager getPackageManager() {
        return mActivity.getPackageManager();
    }


    @Override
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        Log.i(TAG, "handleDecode: " + rawResult.getText());
        mInactivityTimer.onActivity();
        if (null != mOnScannerListener) {
            mOnScannerListener.onDecode(rawResult, barcode, scaleFactor);
        }
    }

    /**
     * 返回预览状态
     *
     * @param delayMS 延迟时间
     */
    public void restartPreviewAfterDelay(long delayMS) {
        Log.i(TAG, "restartPreviewAfterDelay: ");
        if (mHandler != null) {
            mHandler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
        if (null != mOnScannerListener) {
            mOnScannerListener.onRestartPreview();
        }
        resetStatusView();
    }

    /**
     * 重置状态控件
     */
    private void resetStatusView() {
        if (null != mOnScannerListener) {
            mOnScannerListener.onResetStatus();
        }
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
        return mActivity;
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
                Intent intent = getActivity().getIntent();
                destory();
                startActivity(intent);
                lastOrientation = orientation;
                Log.i(TAG, "SUCCESS");
            }
        }
    }

    /**
     * 扫描监听器
     */
    public interface OnScannerListener {

        /**
         * 返回预览状态
         */
        void onRestartPreview();

        /**
         * 扫描状态重置
         */
        void onResetStatus();

        /**
         * 扫码成功
         *
         * @param rawResult   扫描结果数据
         * @param barcode     截图bitmap
         * @param scaleFactor
         */
        void onDecode(Result rawResult, Bitmap barcode, float scaleFactor);
    }
}
