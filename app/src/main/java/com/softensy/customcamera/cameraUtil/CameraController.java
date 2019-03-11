package com.softensy.customcamera.cameraUtil;

/**
 * Created by ujujzk on 28.02.2017.
 */

public interface CameraController {
    void runRec();
    void makePhoto();
    void deleteLastVideoSegment ();
    void mergeVideo();
    void delayRec();
    void stopAutoRec();
    void pauseRec();
    void prepareCamera();
    void releaseCamera();
}
