package com.softensy.customcamera.cameraUtil;

import java.io.File;

public interface CameraSupport {
    void prepareCamera();
    void startRecord(File videoFile);
    File stopRecord();
    void releaseCamera();

    void takePicture(File imageFile);
}
