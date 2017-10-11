package cn.yifan.customscannerdemo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.zxing.Result;

import cn.yifan.scanner.impl.Capture;
import cn.yifan.scanner.widget.AbstractFinderView;
import cn.yifan.scanner.widget.ViewfinderView;

/**
 * 扫描界面
 */
public class CaptureTestActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = CaptureTestActivity.class.getSimpleName();
    /**
     * 导航栏
     */
    Toolbar mTitleBar;

    /**
     * 预览控件
     */
    private SurfaceView mSurfaceView;

    /**
     * 扫描框控件
     */
    private ViewfinderView mViewfinderView;

    /**
     * 扫描控制器
     */
    private Capture mCapture;

    /**
     * 手电筒是否已开启
     */
    private boolean isFlashOpen;

    /**
     * 菜单对象
     */
    private Menu mMenu;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化控件对象
        mTitleBar = (Toolbar) findViewById(R.id.titlebar);
        mSurfaceView = (SurfaceView) findViewById(R.id.sv_capture);
        mViewfinderView = (ViewfinderView) findViewById(R.id.vfv_capture);
        setSupportActionBar(mTitleBar);

        mViewfinderView.setTipsText("扫码快递件Label条码");
        mViewfinderView.setInputText("手动输入");

        mCapture = new Capture(this, mSurfaceView, mViewfinderView);
        mCapture.setOnScannerListener(mListener);
        mViewfinderView.setOnInputTextClickListener(new AbstractFinderView.OnInputTextClickListener() {
            @Override
            public void onclick() {
                Toast.makeText(CaptureTestActivity.this, "is click to make input", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCapture.resume(this);
    }

    @Override
    protected void onPause() {
        mCapture.pause(this);
        refreshFlashStatus(false);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCapture.stop(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCapture.destory();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCapture.surfaceCreated(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCapture.surfaceDestroyed(holder);
    }

    /**
     * 扫描监听器
     */
    public Capture.OnScannerListener mListener = new Capture.OnScannerListener() {
        @Override
        public void onRestartPreview() {

        }

        @Override
        public void onResetStatus() {

        }

        @Override
        public void onDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
            AlertDialog dialog = new AlertDialog.Builder(CaptureTestActivity.this)
                    .setTitle("扫描结果").setMessage(rawResult.getText())
                    .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mCapture.restartPreviewAfterDelay(10L);
                        }
                    }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //初始化菜单
        getMenuInflater().inflate(R.menu.menu_scanner_activity, menu);
        mMenu = menu;
        //刷新导航栏菜单 手电筒按钮状态
        refreshFlashStatus(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_close_flash_light:
                refreshFlashStatus(false);
                mCapture.closeFlashLight();
                return true;
            case R.id.action_open_flash_light:
                refreshFlashStatus(true);
                mCapture.openFlashLight();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 刷新导航栏菜单 手电筒按钮状态
     */
    private void refreshFlashStatus(boolean isFlashOpen) {
        this.isFlashOpen = isFlashOpen;
        setMenuItemVisible(mMenu, R.id.action_close_flash_light, isFlashOpen);
        setMenuItemVisible(mMenu, R.id.action_open_flash_light, !isFlashOpen);
    }

    /**
     * 隐藏或展示菜单项
     *
     * @param menu
     * @param menuID
     * @param isHide
     */
    private void setMenuItemVisible(Menu menu, @IdRes int menuID, boolean isHide) {
        if (null != menu && menuID > 0) {
            MenuItem menuItem = menu.findItem(menuID);
            if (null != menuItem) {
                menuItem.setVisible(isHide);
            }
        }
    }

}
