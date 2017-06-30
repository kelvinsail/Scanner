# Scanner

一个简单的二维码／条形码扫描库，基于ZXing3.3.0封装，去除多余代码，只保留扫描功能及相关

1.添加Gradle依赖[ ![Download](https://api.bintray.com/packages/kelvinsail/maven/scanner/images/download.svg?version=1.0.3) ](https://bintray.com/kelvinsail/maven/scanner/1.0.3/link)
```Java
dependencies {
    compile 'cn.yifan.scanner:scanner:1.0.3'
}
```

2.使用
实现自定义界面及扫描框，自定义扫描框属性，可在布局中设置

```html
    <attr name="FindViewStyle" format="reference"/>

    <declare-styleable name="AbstractFinderView">
        <!-- 提示文字 -->
        <attr name="tips_text" format="string"/>
        <attr name="tips_text_color" format="color"/>
        <attr name="tips_text_size" format="dimension"/>
        <attr name="tips_text_bottom_padding" format="dimension"/>

        <!-- 手动输入提示文字 -->
        <attr name="input_tips" format="string"/>
        <attr name="input_tips_size" format="dimension"/>
        <attr name="input_tips_color" format="color"/>

        <!-- 边框颜色 -->
        <attr name="corner_frame_color" format="color"/>
        <attr name="corner_frame_width" format="dimension"/>
        <attr name="corner_frame_height" format="dimension"/>

        <!-- 准线颜色 -->
        <attr name="laser_color" format="color"/>
        <attr name="laser_width" format="dimension"/>
        <attr name="laser_padding" format="dimension"/>

        <attr name="mask_color" format="color"/>
        <attr name="result_color" format="color"/>
        <attr name="result_point_color" format="color"/>
        <attr name="scanner_alpha" format="float"/>
    </declare-styleable>
```

界面中自定义布局实现，但必须有surfaceview，并实现SurfaceHolder.Callback接口

```Java
**
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
}
```


加入@檀木丁的部分优化，内容参考http://www.jianshu.com/nb/10290510
