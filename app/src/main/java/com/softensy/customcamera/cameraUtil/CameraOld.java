package com.softensy.customcamera.cameraUtil;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileOutputStream;

public class CameraOld implements CameraSupport {

    Camera camera;
    MediaRecorder mediaRecorder;
    SurfaceHolder surfaceHolder;

    File videoFile;



    public CameraOld(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
    }

    @Override
    public void prepareCamera() {

        camera = Camera.open();
        camera.setDisplayOrientation(90);
        Camera.Parameters parameters = camera.getParameters();
        parameters.set("orientation", "portrait");
        parameters.setRotation(90);
        camera.setParameters(parameters);
        if (camera != null) {
            surfaceHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    try {
                        camera.setPreviewDisplay(holder);
                        camera.startPreview();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                }
            });
        }
    }

    private boolean prepareVideoRecorder(File videoFile) {
        mediaRecorder = new MediaRecorder();
        camera.unlock();

        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        mediaRecorder.setOrientationHint(90);

        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    @Override
    public void startRecord(File videoFile) {
        if (prepareVideoRecorder(videoFile)) {
            this.videoFile = videoFile;
            mediaRecorder.start();
        } else {
            releaseMediaRecorder();
        }
    }

    @Override
    public File stopRecord() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                e.printStackTrace();
                videoFile.delete();
                return null;
            }
            releaseMediaRecorder();
            return videoFile;
        } else {
            return null;
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            camera.lock();
        }
    }

    @Override
    public void releaseCamera() {
        releaseMediaRecorder();
        if (camera != null)
            camera.release();
        camera = null;
    }

    @Override
    public void takePicture(File photoFile) {
        camera.takePicture(null, null, (data, camera1) -> {
            try {
                FileOutputStream fos = new FileOutputStream(photoFile);
                fos.write(data);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            camera1.startPreview();
        });
    }
}
