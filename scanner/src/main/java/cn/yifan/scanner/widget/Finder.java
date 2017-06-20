package cn.yifan.scanner.widget;

import android.graphics.Bitmap;

import com.google.zxing.ResultPoint;

import cn.yifan.scanner.camera.CameraManager;

/**
 * 扫码框控件接口类
 *
 * Created by yifan on 2017/6/19.
 */

public interface Finder {

    /**
     * 设置扫码结果图片资源为空，从而显示扫码框
     */
    void drawViewfinder();

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    void drawResultBitmap(Bitmap barcode);

    /**
     * 添加待绘制的扫描闪烁点
     *
     * @param point
     */
    void addPossibleResultPoint(ResultPoint point);

    /**
     * 设置摄像头管理工具类
     *
     * @param cameraManager
     */
    void setCameraManager(CameraManager cameraManager);
}
