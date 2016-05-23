package com.sample.qrcodereadervision.Activity;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import com.sample.qrcodereadervision.R;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.PermissionUtils;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class QRcodeReader extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName();

    private CameraSource cameraSource;
    /** UIパーツ:プレビュー画面 */
    private SurfaceView cameraView;
    /** UIパーツ:デコード結果 */
    private TextView barcodeInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findView();

        Log.d("tag", TAG);

        startQRcodeReader();
    }

    /**
     * Viewの取得
     */
    private void findView() {
        barcodeInfo = (TextView) findViewById(R.id.code_info);
        cameraView = (SurfaceView) findViewById(R.id.camera_view);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraSource != null) {
            cameraSource.stop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        QRcodeReaderPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    private void startQRcodeReader() {
        Log.d(TAG, "starQRcodeReader");
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .setAutoFocusEnabled(true)
                .build();

        cameraView.getHolder()
                  .addCallback(
                          new SurfaceHolder.Callback() {
                              @Override
                              public void surfaceCreated(SurfaceHolder holder) {
                                  QRcodeReaderPermissionsDispatcher.startCameraSourceWithCheck(QRcodeReader.this);
                              }

                              @Override
                              public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                                  Log.d(TAG, "in surfaceChanged");
                              }

                              @Override
                              public void surfaceDestroyed(SurfaceHolder holder) {
                                  Log.d(TAG, "in surfaceDestroyed");
                                  cameraSource.stop();
                              }
                          });

        //QRコード読み取り
        barcodeDetector.setProcessor(
                new Detector.Processor<Barcode>() {
                    @Override
                    public void release() {
                    }

                    @Override
                    public void receiveDetections(Detector.Detections<Barcode> detections) {
                        final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                        if (barcodes.size() != 0) {
                            barcodeInfo.post(
                                    new Runnable() {
                                        public void run() {
                                            barcodeInfo.setText(
                                                    barcodes.valueAt(0).displayValue
                                            );
                                        }
                                    });
                        }
                    }
                });
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    void startCameraSource() {
        try {
            if (cameraSource != null) {
                cameraSource.start(cameraView.getHolder());
            }
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    @OnPermissionDenied(Manifest.permission.CAMERA)
    void deniedPermission() {
        if (PermissionUtils.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(this, "カメラの起動にに失敗しました。", Toast.LENGTH_SHORT)
                 .show();
        }
    }

    @SuppressWarnings("unused")
    @OnShowRationale(Manifest.permission.CALL_PHONE)
    void showRationalForStorage(final PermissionRequest request) {
        Toast.makeText(this, "カメラの使用許可が必要です", Toast.LENGTH_SHORT)
             .show();
        request.proceed();
    }
}