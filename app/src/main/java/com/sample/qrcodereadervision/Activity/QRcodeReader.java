package com.sample.qrcodereadervision.Activity;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import com.sample.qrcodereadervision.R;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.io.IOException;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class QRcodeReader extends Activity {
    private final String TAG = getClass().getSimpleName();

    private int mWidth;
    private int mHeight;
    private CameraSource cameraSource;
    /** UIパーツ:プレビュー画面 */
    private SurfaceView cameraView;
    /** UIパーツ:デコード結果 */
    private TextView barcodeInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        barcodeInfo = (TextView) findViewById(R.id.code_info);
        cameraView = (SurfaceView) findViewById(R.id.camera_view);

        Log.d("tag",TAG);

        ViewTreeObserver viewTreeObserver = cameraView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mWidth = cameraView.getWidth();
                        mHeight = cameraView.getHeight();
                        ViewTreeObserver viewTreeObserver = cameraView.getViewTreeObserver();
                        //startQRcodeReader();
                        QRcodeReaderPermissionsDispatcher.startQRcodeReaderWithCheck(QRcodeReader.this);
                        if (Build.VERSION.SDK_INT < 16) {
                            viewTreeObserver.removeGlobalOnLayoutListener(this);
                        } else {
                            viewTreeObserver.removeOnGlobalLayoutListener(this);
                        }
                    }
                });
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    public void startQRcodeReader() {
        Log.d(TAG, "starQRcodeReader");
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .setRequestedPreviewSize(mWidth, mHeight)
                .setAutoFocusEnabled(true)
                .build();

        cameraView.getHolder()
                  .addCallback(
                          new SurfaceHolder.Callback() {
                              @Override
                              public void surfaceCreated(SurfaceHolder holder) {
                                  try {
                                      if (ActivityCompat.checkSelfPermission(
                                              getApplicationContext(),
                                              Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                          return;
                                      }
                                      cameraSource.start(cameraView.getHolder());
                                      Log.d(TAG, "started");
                                  } catch (IOException ie) {
                                      Log.e("CAMERA SOURCE", ie.getMessage());
                                  }
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
}