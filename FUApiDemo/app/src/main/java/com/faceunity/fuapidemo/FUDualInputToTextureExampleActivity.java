package com.faceunity.fuapidemo;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.faceunity.fuapidemo.gles.FullFrameRect;
import com.faceunity.fuapidemo.gles.Texture2dProgram;
import com.faceunity.wrapper.faceunity;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 这个Activity演示了从Camera取数据,用fuDualInputToTexure处理并预览展示
 * 所谓dualinput，指从cpu和gpu同时拿数据，
 * cpu拿到的是nv21的byte数组，gpu拿到的是对应的texture
 *
 * Created by lirui on 2016/12/13.
 */

@SuppressWarnings("deprecation")
public class FUDualInputToTextureExampleActivity extends Activity
        implements Camera.PreviewCallback,
                   SurfaceTexture.OnFrameAvailableListener {

    final String TAG = "FUDualInputToTextureEg";

    Camera mCamera;

    GLSurfaceView glSf;

    Handler mMainHandler;

    int cameraWidth = 1280;
    int cameraHeight = 720;

    byte[] mCameraNV21Byte;
    int mFrameId = 0;

    int mFacebeautyItem; //美颜道具
    int mEffectItem; //道具

    long resumeTimeStamp;
    boolean isFirstOnFrameAvailable;
    long frameAvailableTimeStamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fudualinputtotexture_example);

        mMainHandler = new MainHandler(this);

        glSf = (GLSurfaceView) findViewById(R.id.glsv);
        glSf.setEGLContextClientVersion(2);
        glSf.setRenderer(new GLRenderer());
        glSf.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    protected void onResume() {
        Log.e(TAG, "onResume");

        resumeTimeStamp = System.currentTimeMillis();
        isFirstOnFrameAvailable = true;

        super.onResume();

        openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT,
                cameraWidth,
                cameraHeight);

        /**
         * 请注意这个地方
         */
        Camera.Size size = mCamera.getParameters().getPreviewSize();
        cameraWidth = size.width;
        cameraHeight = size.height;
        Log.e(TAG, "open camera size " + size.width + " " + size.height);

        AspectFrameLayout aspectFrameLayout = (AspectFrameLayout) findViewById(R.id.afl);
        aspectFrameLayout.setAspectRatio(1.0f * cameraHeight / cameraWidth);

        glSf.onResume();
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();

        releaseCamera();

        glSf.onPause();

        mFrameId = 0;

        faceunity.fuDestroyItem(mEffectItem);
        faceunity.fuDestroyItem(mFacebeautyItem);
        faceunity.fuOnDeviceLost();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!true) {
            Log.e(TAG, "onPreviewFrame");
        }
        Log.e(TAG, "onPreviewThread " + Thread.currentThread());
        mCameraNV21Byte = data;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (isFirstOnFrameAvailable) {
            frameAvailableTimeStamp = System.currentTimeMillis();
            isFirstOnFrameAvailable = false;
            Log.e(TAG, "first frame available time cost " +
                    (frameAvailableTimeStamp - resumeTimeStamp));
        }
        if (!true) {
            Log.e(TAG, "onFrameAvailable");
        }
        glSf.requestRender();
    }

    private void handleCameraStartPreview(SurfaceTexture surfaceTexture) {
        Log.e(TAG, "handleCameraStartPreview");
        mCamera.setPreviewCallback(this);
        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        surfaceTexture.setOnFrameAvailableListener(this);
        mCamera.startPreview();
    }

    class GLRenderer implements GLSurfaceView.Renderer {

        FullFrameRect mFullScreenFUDisplay;
        FullFrameRect mFullScreenCamera;

        int mCameraTextureId;
        SurfaceTexture mCameraSurfaceTexture;

        float mFacebeautyColorLevel = 1.0f;
        float mFacebeautyBlurLevel = 8.0f;
        String mFilterName = "nature";

        boolean isFirstOnDrawFrame;

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.e(TAG, "onSurfaceCreated");
            mFullScreenCamera = new FullFrameRect(new Texture2dProgram(
                    Texture2dProgram.ProgramType.TEXTURE_EXT));
            mCameraTextureId = mFullScreenCamera.createTextureObject();
            mCameraSurfaceTexture = new SurfaceTexture(mCameraTextureId);
            mMainHandler.sendMessage(mMainHandler.obtainMessage(
                    MainHandler.HANDLE_CAMERA_START_PREVIEW,
                    mCameraSurfaceTexture));

            mFullScreenFUDisplay = new FullFrameRect(new Texture2dProgram(
                    Texture2dProgram.ProgramType.TEXTURE_2D));

            try {
                InputStream is = getAssets().open("v3.mp3");
                byte[] v3data = new byte[is.available()];
                is.read(v3data);
                is.close();
                faceunity.fuSetup(v3data, null, authpack.A());
                faceunity.fuSetMaxFaces(1);
                Log.e(TAG, "fuSetup");

                is = getAssets().open("face_beautification.mp3");
                byte[] itemData = new byte[is.available()];
                is.read(itemData);
                is.close();
                mFacebeautyItem = faceunity.fuCreateItemFromPackage(itemData);

                is = getAssets().open("YellowEar.mp3");
                itemData = new byte[is.available()];
                is.read(itemData);
                is.close();
                mEffectItem = faceunity.fuCreateItemFromPackage(itemData);
            } catch (IOException e) {
                e.printStackTrace();
            }

            isFirstOnDrawFrame = true;
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Log.e(TAG, "onSurfaceChanged " + width + " " + height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (!true) {
                Log.e(TAG, "onDrawFrame");
            }

            if (isFirstOnDrawFrame) {
                //第一次onDrawFrame并不是由camera内容驱动的
                isFirstOnDrawFrame = false;
                return;
            }

            float[] mtx = new float[16];
            mCameraSurfaceTexture.updateTexImage();
            mCameraSurfaceTexture.getTransformMatrix(mtx);
            //mFullScreenCamera.drawFrame(mCameraTextureId, mtx);

            int isTracking = faceunity.fuIsTracking();
            if (true) {
                Log.e(TAG, "isTracking " + isTracking);
            }

            faceunity.fuItemSetParam(mEffectItem, "isAndroid", 1.0);

            faceunity.fuItemSetParam(mFacebeautyItem, "color_level", mFacebeautyColorLevel);
            faceunity.fuItemSetParam(mFacebeautyItem, "blur_radius", mFacebeautyBlurLevel);
            faceunity.fuItemSetParam(mFacebeautyItem, "filter_name", mFilterName);

            /**
             * 这里拿到fu处理过后的texture，可以对这个texture做后续操作，如硬编、预览。
             */
            boolean isOESTexture = true;
            int flags = isOESTexture ? faceunity.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE : 0;
            int fuTex = faceunity.fuDualInputToTexture(mCameraNV21Byte, mCameraTextureId, flags,
                    cameraWidth, cameraHeight, mFrameId++, new int[] {mEffectItem, mFacebeautyItem});


            mFullScreenFUDisplay.drawFrame(fuTex, mtx);
        }
    }

    static class MainHandler extends Handler {

        static final int HANDLE_CAMERA_START_PREVIEW = 1;

        private WeakReference<FUDualInputToTextureExampleActivity> mActivityWeakReference;

        MainHandler(FUDualInputToTextureExampleActivity activity) {
            mActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            FUDualInputToTextureExampleActivity activity = mActivityWeakReference.get();
            switch (msg.what) {
                case HANDLE_CAMERA_START_PREVIEW:
                    activity.handleCameraStartPreview((SurfaceTexture) msg.obj);
                    break;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void openCamera(int cameraType, int desiredWidth, int desiredHeight) {
        Log.d(TAG, "openCamera");
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        int cameraId = 0;
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == cameraType) {
                cameraId = i;
                mCamera = Camera.open(i);
                break;
            }
        }
        if (mCamera == null) {
            throw new RuntimeException("unable to open camera");
        }

        CameraUtils.setCameraDisplayOrientation(this, cameraId, mCamera);

        Camera.Parameters parameters = mCamera.getParameters();
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        mCamera.setDisplayOrientation(90);
        CameraUtils.choosePreviewSize(parameters, desiredWidth, desiredHeight);
        mCamera.setParameters(parameters);
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            try {
                mCamera.setPreviewTexture(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.release();
            mCamera = null;
            Log.e(TAG, "release camera");
        }
    }

}
