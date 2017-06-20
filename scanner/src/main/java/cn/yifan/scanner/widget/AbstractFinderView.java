package cn.yifan.scanner.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import com.google.zxing.ResultPoint;

import java.util.ArrayList;
import java.util.List;

import cn.yifan.scanner.R;
import cn.yifan.scanner.camera.CameraManager;

/**
 * FinderViews抽象基类
 *
 * Created by yifan on 2017/6/19.
 */
public abstract class AbstractFinderView extends View implements Finder {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 80L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;
    private static final int PAINT_STROKE_WIDTH = 6;
    private static final int CORNER_FRAME_HEIGHT = 32;
    private static final String TAG = "AbstractFinderView";

    /**
     * 提示文本颜色
     */
    private String mTipsText;
    private int mTipsTextColor;
    private int mTipsTextSize;
    private int mTipsTextBottomPadding;


    /**
     * 摄像头管理工具
     */
    private CameraManager mCameraManager;

    /**
     * 画笔
     */
    private Paint mPaint;

    /**
     * 扫描结果bitmap
     */
    private Bitmap mResultBitmap;

    /**
     * 边框
     */
    private int mCornerFrameColor;
    private int mCornerFrameWidth;
    private int mCornerFrameHeight;

    /**
     * 准线
     */
    private int mLaserColor;
    private int mLaserWidth;
    private int mLaserPadding;
    /**
     * 背景
     */
    private int mMaskColor;
    /**
     * 扫描结果
     */
    private int mResultColor;
    private int mResultPointColor;
    /**
     * 准线透明度
     */
    private int mScannerAlpha;
    private List<ResultPoint> mPossibleResultPoints;
    private List<ResultPoint> mLastPossibleResultPoints;

    public AbstractFinderView(Context context) {
        this(context, null);
    }

    public AbstractFinderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbstractFinderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    /**
     * 初始化
     */
    private void init(AttributeSet attrs, int defStyleAttr) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setDither(true);
        Log.i(TAG, "init: " + mPaint.getStrokeWidth());
        Resources resources = getResources();
        TypedArray array = getContext().obtainStyledAttributes(attrs,
                R.styleable.AbstractFinderView, defStyleAttr, R.style.FinderViewStyle);
        //提示文本
        mTipsText = "这是提示文字,识别扫描";
        mTipsTextColor = array.getColor(
                R.styleable.AbstractFinderView_tips_text_color, resources.getColor(R.color.tips_text));
        mTipsTextSize = array.getDimensionPixelSize(
                R.styleable.AbstractFinderView_tips_text_size, resources.getDimensionPixelSize(R.dimen.tips_text_size));
        mTipsTextBottomPadding = array.getDimensionPixelSize(
                R.styleable.AbstractFinderView_tips_text_bottom_padding, resources.getDimensionPixelSize(R.dimen.tips_text_bottom_padding));
        //背景颜色
        mMaskColor = array.getColor(R.styleable.AbstractFinderView_mask_color,
                resources.getColor(R.color.viewfinder_mask));

        mCornerFrameColor = array.getColor(R.styleable.AbstractFinderView_corner_frame_color,
                resources.getColor(R.color.viewfinder_laser));
        mCornerFrameWidth = array.getDimensionPixelSize(R.styleable.AbstractFinderView_corner_frame_width, PAINT_STROKE_WIDTH);
        mCornerFrameHeight = array.getDimensionPixelSize(R.styleable.AbstractFinderView_corner_frame_height,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CORNER_FRAME_HEIGHT, resources.getDisplayMetrics()));

        mLaserColor = array.getColor(R.styleable.AbstractFinderView_laser_color,
                resources.getColor(R.color.viewfinder_laser));
        mLaserWidth = array.getDimensionPixelSize(R.styleable.AbstractFinderView_laser_width, PAINT_STROKE_WIDTH);
        mLaserPadding = array.getDimensionPixelSize(R.styleable.AbstractFinderView_laser_padding, 0);

        mResultColor = array.getColor(R.styleable.AbstractFinderView_result_color,
                resources.getColor(R.color.result_view));
        mResultPointColor = array.getColor(R.styleable.AbstractFinderView_result_point_color,
                resources.getColor(R.color.possible_result_points));
        mScannerAlpha = (int) (255 * array.getFloat(R.styleable.AbstractFinderView_scanner_alpha, 0.7f));
        array.recycle();

        mPossibleResultPoints = new ArrayList<>(5);
        mLastPossibleResultPoints = null;
    }

    @Override
    public void drawViewfinder() {
        Bitmap resultBitmap = this.mResultBitmap;
        this.mResultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    @Override
    public void drawResultBitmap(Bitmap barcode) {
        mResultBitmap = barcode;
        invalidate();
    }

    @Override
    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = mPossibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

    @Override
    public void setCameraManager(CameraManager cameraManager) {
        this.mCameraManager = cameraManager;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (null == mCameraManager) {
            return;
        }
        Rect frame = mCameraManager.getFramingRect();
        Rect previewFrame = mCameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        mPaint.setColor(mResultBitmap != null ? mResultColor : mMaskColor);
        canvas.drawRect(0, 0, width, frame.top, mPaint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, mPaint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, mPaint);
        canvas.drawRect(0, frame.bottom + 1, width, height, mPaint);

        if (mResultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            mPaint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(mResultBitmap, null, frame, mPaint);
        } else {

            // Draw a red "laser scanner" line through the middle to show decoding is active
            mPaint.setColor(mLaserColor);
            mPaint.setAlpha(SCANNER_ALPHA[mScannerAlpha]);
            mScannerAlpha = (mScannerAlpha + 1) % SCANNER_ALPHA.length;
            int middle = frame.height() / 2 + frame.top;
            canvas.drawRect(frame.left + mLaserPadding, middle - mLaserWidth / 2,
                    frame.right - mLaserPadding, middle + mLaserWidth / 2, mPaint);

            float scaleX = frame.width() / (float) previewFrame.width();
            float scaleY = frame.height() / (float) previewFrame.height();

            List<ResultPoint> currentPossible = mPossibleResultPoints;
            List<ResultPoint> currentLast = mLastPossibleResultPoints;
            int frameLeft = frame.left;
            int frameTop = frame.top;
            if (currentPossible.isEmpty()) {
                mLastPossibleResultPoints = null;
            } else {
                mPossibleResultPoints = new ArrayList<>(5);
                mLastPossibleResultPoints = currentPossible;
                mPaint.setAlpha(CURRENT_POINT_OPACITY);
                mPaint.setColor(mResultPointColor);
                synchronized (currentPossible) {
                    for (ResultPoint point : currentPossible) {
                        canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                                frameTop + (int) (point.getY() * scaleY),
                                POINT_SIZE, mPaint);
                    }
                }
            }
            if (currentLast != null) {
                mPaint.setAlpha(CURRENT_POINT_OPACITY / 2);
                mPaint.setColor(mResultPointColor);
                synchronized (currentLast) {
                    float radius = POINT_SIZE / 2.0f;
                    for (ResultPoint point : currentLast) {
                        canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                                frameTop + (int) (point.getY() * scaleY),
                                radius, mPaint);
                    }
                }
            }
            //绘制边框 , '+ 2'填充缝隙
            mPaint.setColor(mCornerFrameColor);
            //垂直
            canvas.drawRect(frame.left, frame.top, frame.left + mCornerFrameWidth, frame.top + mCornerFrameHeight, mPaint);
            canvas.drawRect(frame.right - mCornerFrameWidth, frame.top, frame.right + 2, frame.top + mCornerFrameHeight, mPaint);
            canvas.drawRect(frame.left, frame.bottom - mCornerFrameHeight, frame.left + mCornerFrameWidth, frame.bottom, mPaint);
            canvas.drawRect(frame.right - mCornerFrameWidth, frame.bottom - mCornerFrameHeight, frame.right + 2, frame.bottom, mPaint);
            //水平
            canvas.drawRect(frame.left, frame.top, frame.left + mCornerFrameHeight, frame.top + mCornerFrameWidth, mPaint);
            canvas.drawRect(frame.right - mCornerFrameHeight, frame.top, frame.right, frame.top + mCornerFrameWidth, mPaint);
            canvas.drawRect(frame.left, frame.bottom - mCornerFrameWidth, frame.left + mCornerFrameHeight, frame.bottom + 2, mPaint);
            canvas.drawRect(frame.right - mCornerFrameHeight, frame.bottom - mCornerFrameWidth, frame.right, frame.bottom + 2, mPaint);

            //tips text
            if (!TextUtils.isEmpty(mTipsText)) {
                mPaint.setTextSize(mTipsTextSize);
                mPaint.setColor(mTipsTextColor);
                mPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(mTipsText, 0, mTipsText.length(), frame.centerX(),
                        frame.top - (mTipsTextBottomPadding + mTipsTextSize / 2), mPaint);
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY,
                    frame.left - POINT_SIZE,
                    frame.top - POINT_SIZE,
                    frame.right + POINT_SIZE,
                    frame.bottom + POINT_SIZE);
        }
    }

    public void setMaskColor(int maskColor) {
        this.mMaskColor = maskColor;
        invalidate();
    }

    public void setResultColor(int resultColor) {
        this.mResultColor = resultColor;
        invalidate();
    }

    public void setLaserColor(int laserColor) {
        this.mLaserColor = laserColor;
        invalidate();
    }
}
