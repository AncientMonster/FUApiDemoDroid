package com.faceunity.fuapidemo;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;

import com.faceunity.wrapper.faceunity;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 这个Activity演示了如何通过fuRenderToNV21Image
 * 实现在无GL Context的情况下输入nv21的人脸图像，输出添加道具及美颜后的nv21图像
 * 这个Activity只演示API的使用，无预览效果，
 * FU SDK使用者可以将拿到处理后的nv21图像与自己的原有项目对接。
 * 请FU SDK使用者直接参考示例放至代码至对应位置
 *
 * 本示例的nv21图像输入源为Android Camera的onPreviewFrame callback,
 * 此callback在没有预览时，有些手机不会回调，请开发者注意。
 * 单身因为FU SDK使用者也许并无需关心camera数据来源，且本Demo
 * 只是演示API的使用，这里就不做处理了。
 *
 * FU SDK与camera无耦合，不关心数据的来源，只要内容正确且和
 * 宽高吻合即可。
 *
 * Created by lirui on 2016/12/13.
 */

@SuppressWarnings("deprecation")
public class FURenderToNV21ImageExampleActivity extends Activity
        implements Camera.PreviewCallback {

    final String TAG = "FURenderToNV21Image";
    Camera mCamera;

    int cameraWidth;
    int cameraHeight;

    int mFacebeautyItem;
    int mEffectItem;

    int mFrameId;

    float mFacebeautyColorLevel = 1.0f;
    float mFacebeautyBlurLevel = 8.0f;
    String mFilterName = "nature";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_furendertonv21image_example);
    }

    @Override
    protected void onResume() {
        super.onResume();

        cameraWidth = 1280;
        cameraHeight = 720;
        openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT,
                cameraWidth,
                cameraHeight);

        Camera.Size size = mCamera.getParameters().getPreviewSize();
        cameraWidth = size.width;
        cameraHeight = size.height;
        Log.e(TAG, "open camera size " + size.width + " " + size.height);

        fuInit();

        handleCameraStartPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();

        mFrameId = 0;

        faceunity.fuDestroyItem(mEffectItem);
        faceunity.fuDestroyItem(mFacebeautyItem);
        faceunity.fuOnDeviceLost();
    }

    /**
     * 初始化GL环境，创建GL Context
     *
     */
    private void fuInit() {
        /**
         * 如果当前线程没有GL Context，那么可以使用我们的API创建一个
         * 如果已经有GL Context，如在GLSufaceView对应的Renderer,则无需使用
         *
         * 所有FU API 都需要保证在*同一个*具有*GL Context*的线程被调用
         *
         * 建议使用者在*非主线程*完成fu相关操作，这里就不做演示了
         */
        faceunity.fuCreateEGLContext();

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
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.e(TAG, "onPreviewFrame");
        int isTracking = faceunity.fuIsTracking();
        if (true) {
            Log.e(TAG, "isTracking " + isTracking);
        }

        faceunity.fuItemSetParam(mEffectItem, "isAndroid", 1.0);

        faceunity.fuItemSetParam(mFacebeautyItem, "color_level", mFacebeautyColorLevel);
        faceunity.fuItemSetParam(mFacebeautyItem, "blur_radius", mFacebeautyBlurLevel);
        faceunity.fuItemSetParam(mFacebeautyItem, "filter_name", mFilterName);

        faceunity.fuRenderToNV21Image(data, cameraWidth, cameraHeight, mFrameId++, new int[] { mEffectItem, mFacebeautyItem });
    }

    private void handleCameraStartPreview() {
        Log.e(TAG, "handleCameraStartPreview");
        mCamera.setPreviewCallback(this);
        mCamera.startPreview();
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
