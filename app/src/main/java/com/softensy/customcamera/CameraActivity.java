package com.softensy.customcamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import com.softensy.customcamera.databinding.ActivityCameraBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//http://stackoverflow.com/questions/20325615/android-concatenate-two-videos

public class CameraActivity extends AppCompatActivity {


    private static final int PERMISSIONS_REQUEST = 12345;
    ActivityCameraBinding binding;

    Camera camera;
    MediaRecorder mediaRecorder;

    File path;
    File videoFile;

    boolean flashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera);

        initBtns();
    }

    private void initBtns() {
        binding.exitBtn.setOnClickListener(v -> finish());

        binding.recordBtn.setOnTouchListener((v, event) -> record(event));
        binding.cameraBtn.setOnClickListener(v -> takePicture());
        binding.flashBtn.setOnClickListener(v -> switchFlash());
    }

    private void switchFlash() {
        if (flashOn) {
            flashOn = false;
            binding.flashBtn.setImageResource(R.drawable.ic_flash_on_white_30dp);
        } else {
            flashOn = true;
            binding.flashBtn.setImageResource(R.drawable.ic_flash_off_white_30dp);

        }
    }

    private void prepareCamera() {
        path = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString(), getString(R.string.app_name) + "/");
        path.mkdirs();

        String fileName = new SimpleDateFormat("yMMdd_HHmmss", Locale.getDefault()).format(new Date());
        videoFile = new File(path, "video" + fileName + ".3gp");

        camera = Camera.open();
        camera.setDisplayOrientation(90);
        Camera.Parameters parameters = camera.getParameters();
        parameters.set("orientation", "portrait");
        parameters.setRotation(90);
        camera.setParameters(parameters);
        if (camera != null) {
            binding.cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
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

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO
                    },
                    PERMISSIONS_REQUEST);
        } else {
            prepareCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 2 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    prepareCamera();
                } else {
                    finish();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();
        if (camera != null)
            camera.release();
        camera = null;
    }

    Handler h = new Handler();

    Runnable run = () -> {
        if (prepareVideoRecorder()) {
            mediaRecorder.start();
        } else {
            releaseMediaRecorder();
        }
    };


    public boolean record(MotionEvent event) {


        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            binding.recordBtn.setImageResource(R.drawable.ic_record_pressed);
            h.postDelayed(run, 500);
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            h.removeCallbacks(run);
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                releaseMediaRecorder();
            }
            binding.recordBtn.setImageResource(R.drawable.ic_record);
            return true;
        }

        return false;
    }

    public void takePicture() {
        camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try {
                    String fileName = new SimpleDateFormat("'photo'yMMdd_HHmmss'.jpg'", Locale.getDefault()).format(new Date());
                    File photoFile = new File(path, fileName);
                    FileOutputStream fos = new FileOutputStream(photoFile);
                    fos.write(data);
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                camera.startPreview();
            }
        });

    }

    private boolean prepareVideoRecorder() {

        mediaRecorder = new MediaRecorder();
        camera.unlock();

        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile
                .get(CamcorderProfile.QUALITY_HIGH));
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mediaRecorder.setPreviewDisplay(binding.cameraView.getHolder().getSurface());
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

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            camera.lock();
        }
    }
}
