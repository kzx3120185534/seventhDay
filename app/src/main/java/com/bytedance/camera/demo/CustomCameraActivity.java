package com.bytedance.camera.demo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static com.bytedance.camera.demo.utils.Utils.MEDIA_TYPE_IMAGE;
import static com.bytedance.camera.demo.utils.Utils.MEDIA_TYPE_VIDEO;
import static com.bytedance.camera.demo.utils.Utils.getOutputMediaFile;

public class CustomCameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = "CustomCameraActivity";

    /**
     * 单次变焦变化量
     */
    public static final int ZOOM_CHANGE_SCALE = 5;
    /**
     * 初始焦距
     */
    public static final int ZOOM_INIT_VALUE = 0;

    private SurfaceView mSurfaceView;
    private Camera mCamera;



    private int CAMERA_TYPE = Camera.CameraInfo.CAMERA_FACING_BACK;

    private boolean isRecording = false;

    private int rotationDegree = 0;
    private SurfaceHolder mSurfaceHolder;
    private File outputMediaFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_custom_camera);

        mSurfaceView = findViewById(R.id.img);
        //todo 给SurfaceHolder添加Callback

        //初始化摄像头对象
        mCamera = getCamera(CAMERA_TYPE);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);

        findViewById(R.id.btn_picture).setOnClickListener(v -> {
            //todo 拍一张照片
            Log.i(TAG, "onCreate: take picture");
            mCamera.takePicture(null, null, mPicture);

        });

        findViewById(R.id.btn_record).setOnClickListener(v -> {
            //todo 录制，第一次点击是start，第二次点击是stop
            if (isRecording) {
                //todo 停止录制
                releaseMediaRecorder();
                isRecording = false;
            } else {
                //todo 录制
                isRecording = true;
                prepareVideoRecorder();


            }
        });

        findViewById(R.id.btn_facing).setOnClickListener(v -> {
            //todo 切换前后摄像头
            if(CAMERA_TYPE == Camera.CameraInfo.CAMERA_FACING_BACK)
                mCamera = getCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
            else
                mCamera = getCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
            //切换了摄像头需要重新配置一遍surface与camera之间的联系
            mSurfaceHolder.addCallback(this);
            startPreview(mSurfaceHolder);

        });

        findViewById(R.id.btn_zoom).setOnClickListener(v -> {
            //todo 调焦，需要判断手机是否支持
            //判断是否支持调整焦距
            if (mCamera.getParameters().isSmoothZoomSupported())
            {
                Camera.Parameters params = mCamera.getParameters();
                final int MAX = params.getMaxZoom();
                if(MAX==0)
                    return;

                int zoomValue = params.getZoom();
                //未达到变焦上限，则放大焦距
                if(zoomValue <= MAX-ZOOM_CHANGE_SCALE){
                    zoomValue += ZOOM_CHANGE_SCALE;
                    params.setZoom(zoomValue);

                }else{
                    //达到最大后回到原始焦距
                    params.setZoom(ZOOM_INIT_VALUE);
                }

                mCamera.setParameters(params);

            }

        });
    }

    public Camera getCamera(int position) {
        CAMERA_TYPE = position;
        if (mCamera != null) {
            releaseCameraAndPreview();
        }
        Camera cam = Camera.open(position);

        //todo 摄像头添加属性，例是否自动对焦，设置旋转方向等
        //旋转
        cam.setDisplayOrientation(getCameraDisplayOrientation(CAMERA_TYPE));
        //自动对焦
        Camera.Parameters parameters = cam.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        cam.setParameters(parameters);
        return cam;
    }


    private static final int DEGREE_90 = 90;
    private static final int DEGREE_180 = 180;
    private static final int DEGREE_270 = 270;
    private static final int DEGREE_360 = 360;

    private int getCameraDisplayOrientation(int cameraId) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = DEGREE_90;
                break;
            case Surface.ROTATION_180:
                degrees = DEGREE_180;
                break;
            case Surface.ROTATION_270:
                degrees = DEGREE_270;
                break;
            default:
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % DEGREE_360;
            result = (DEGREE_360 - result) % DEGREE_360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + DEGREE_360) % DEGREE_360;
        }
        return result;
    }


    private void releaseCameraAndPreview() {
        //todo 释放camera资源
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    Camera.Size size;

    private void startPreview(SurfaceHolder holder) {
        //todo 开始预览
        //将摄像头数据提供预览
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private MediaRecorder mMediaRecorder;

    private boolean prepareVideoRecorder() {
        //todo 准备MediaRecorder
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        mMediaRecorder.setPreviewDisplay(mSurfaceView.getHolder().getSurface());
        mMediaRecorder.setOrientationHint(rotationDegree);

        outputMediaFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);

        try{
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (IOException e) {
            releaseMediaRecorder();
        }
        return true;
    }


    private void releaseMediaRecorder() {
        //todo 释放MediaRecorder

        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mMediaRecorder = null;
        mCamera.lock();

        //录制完成后，刷新媒体库
        if(outputMediaFile != null){
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(outputMediaFile));
            this.sendBroadcast(mediaScanIntent);
        }


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //todo 释放Camera和MediaRecorder资源
        if(mCamera != null)
            releaseCameraAndPreview();
        if(mMediaRecorder != null)
            releaseMediaRecorder();
    }


    private Camera.PictureCallback mPicture = (data, camera) -> {
        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

        if (pictureFile == null) {
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();

            //扫描媒体库更新图片
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(pictureFile));
            this.sendBroadcast(mediaScanIntent);
        } catch (IOException e) {
            Log.d("mPicture", "Error accessing file: " + e.getMessage());
        }

        mCamera.startPreview();
    };


    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = Math.min(w, h);

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

}
