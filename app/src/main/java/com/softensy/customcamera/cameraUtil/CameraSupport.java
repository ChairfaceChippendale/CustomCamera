package com.softensy.customcamera.cameraUtil;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.softensy.customcamera.VideoSegment;

import java.io.File;

public interface CameraSupport {
    void prepareCamera();
    void startRecord(File videoFile);
    File stopRecord();
    void releaseCamera();

    void takePicture(File imageFile);
}
