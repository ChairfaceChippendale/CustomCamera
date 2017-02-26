package com.softensy.customcamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;

import com.softensy.customcamera.cameraUtil.CameraOld;
import com.softensy.customcamera.cameraUtil.CameraSupport;
import com.softensy.customcamera.databinding.ActivityCameraBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


//http://stackoverflow.com/questions/26035547/how-to-pause-resume-the-video-recording

public class CameraActivity extends AppCompatActivity {


    static final int MIN_VIDEO_DURATION_SEC = 2;
    static final int MAX_VIDEO_DURATION_SEC = 10;

    ActivityCameraBinding binding;

    CameraSupport cameraSupport;

    boolean flashOn = false;

    Handler recordPressHandler = new Handler();
    Runnable runRecord = () -> cameraSupport.startRecord(provideFileToRecord());

    File videoFile;
    int videoFragmentCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera);

        cameraSupport = new CameraOld(binding.cameraView.getHolder());

        initBtns();
    }

    File provideFileToRecord() {

        File path = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString(), getString(R.string.app_name) + "/");
        path.mkdirs();

        String fileName = new SimpleDateFormat("'video_'yMMdd_HHmmss'.3gp'", Locale.getDefault()).format(new Date());
        return new File(path, fileName);
    }

    private void initBtns() {
        binding.exitBtn.setOnClickListener(v -> finish());
        binding.recordBtn.setOnTouchListener((v, event) -> onRecordPressed(event.getAction()));
        binding.cameraBtn.setOnClickListener(v -> onCameraPressed());
        binding.flashBtn.setOnClickListener(v -> onFlashPressed());
        binding.deleteBtn.setOnClickListener(v -> {
        });//TODO delete previous video frgment and seconds of record
        binding.confirmBtn.setOnClickListener(v -> {
        });//TODO make united video file from fragments
    }

    private void onFlashPressed() {
        if (flashOn) {
            flashOn = false;
            binding.flashBtn.setImageResource(R.drawable.ic_flash_on_white_30dp);
        } else {
            flashOn = true;
            binding.flashBtn.setImageResource(R.drawable.ic_flash_off_white_30dp);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraSupport.prepareCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraSupport.releaseCamera();
    }

    public boolean onRecordPressed(int action) {

        if (action == MotionEvent.ACTION_DOWN) {
            //delay in 200 mlsec to avoid click (short press) on record button
            recordPressHandler.postDelayed(runRecord, 200);

            binding.recordBtn.setImageResource(R.drawable.ic_record_pressed);
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            recordPressHandler.removeCallbacks(runRecord);

            cameraSupport.stopRecord();

            binding.recordBtn.setImageResource(R.drawable.ic_record);
            return true;
        }
        return false;
    }

    public void onCameraPressed() {
        File path = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString(), getString(R.string.app_name) + "/");
        path.mkdirs();
        String fileName = new SimpleDateFormat("'photo_'yMMdd_HHmmss'.jpg'", Locale.getDefault()).format(new Date());
        File photoFile = new File(path, fileName);
        cameraSupport.takePicture(photoFile);
    }


}
