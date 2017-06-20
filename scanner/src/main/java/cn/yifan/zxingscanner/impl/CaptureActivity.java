package cn.yifan.zxingscanner.impl;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Handler;

import com.google.zxing.Result;

import cn.yifan.zxingscanner.widget.ViewfinderView;
import cn.yifan.zxingscanner.camera.CameraManager;

/**
 * Created by wuyifan on 2017/6/19.
 */

public interface CaptureActivity {

    void drawViewfinder();

    void startActivity(Intent intent);

    PackageManager getPackageManager();

    void destory();

    void setResult(int resultOk, Intent obj);

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult   The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode     A greyscale bitmap of the camera data which was decoded.
     */
    void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor);

    CameraManager getCameraManager();

    ViewfinderView getViewfinderView();

    Handler getHandler();

    Activity getActivity();
}
