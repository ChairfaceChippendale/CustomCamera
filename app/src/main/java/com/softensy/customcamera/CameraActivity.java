package com.softensy.customcamera;

import android.databinding.DataBindingUtil;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.softensy.customcamera.cameraUtil.CameraOld;
import com.softensy.customcamera.cameraUtil.CameraSupport;
import com.softensy.customcamera.databinding.ActivityCameraBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Vector;


//http://stackoverflow.com/questions/26035547/how-to-pause-resume-the-video-recording

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = CameraActivity.class.getSimpleName();

    static final int MIN_VIDEO_DURATION_MILLISEC = 2000;
    static final int MAX_VIDEO_DURATION_MILLISEC = 10000;
    static final int STEP_VIDEO_DURATION_MILLISEC = 100;

    private long totalVideoDuration = 0L;
    private long startHTime = 0L;
    private long segmentDurationInMillisec = 0L;
    private long timeSwapBuff = 0L;
    boolean timelineIsRed = true;


    private Handler customHandler = new Handler();
    private Runnable updateTimer = new Runnable() {
        public void run() {

            segmentDurationInMillisec = SystemClock.uptimeMillis() - startHTime;
            totalVideoDuration = timeSwapBuff + segmentDurationInMillisec;
            binding.timeline.setProgress((int) totalVideoDuration);

            if (totalVideoDuration > MIN_VIDEO_DURATION_MILLISEC && timelineIsRed){
                //"timelineIsRed" prevents changing of timeline color too often
                binding.timeline.setProgressDrawable(getResources().getDrawable(R.drawable.progress));
                timelineIsRed = false;
            }

            if (totalVideoDuration >= MAX_VIDEO_DURATION_MILLISEC) {
                cameraSupport.stopRecord();
                customHandler.removeCallbacks(updateTimer);
            } else {
                customHandler.postDelayed(this, STEP_VIDEO_DURATION_MILLISEC);
            }

        }
    };


    ActivityCameraBinding binding;

    CameraSupport cameraSupport;

    boolean flashOn = false;


    Queue<VideoSegment> videoSegments;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera);
        cameraSupport = new CameraOld(binding.cameraView.getHolder());
        videoSegments = new LinkedList<>();
        initBtns();
    }

    File provideFileToRecord() {

        File path = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString(), getString(R.string.app_name) + "/");
        path.mkdirs();

        String fileName = new SimpleDateFormat("'video_'yMMdd_HHmmss'.3gp'", Locale.getDefault()).format(new Date());
        File f = new File(path, fileName);
        Log.d(TAG, "videoFile: " + f.getAbsolutePath());
        return f;
    }



    private boolean onRecordPressed(int action) {

        if (action == MotionEvent.ACTION_DOWN) {
            if (totalVideoDuration <= MAX_VIDEO_DURATION_MILLISEC) {

                cameraSupport.startRecord(provideFileToRecord());
                startHTime = SystemClock.uptimeMillis();
                customHandler.postDelayed(updateTimer, 0);

            } else {
                Toast.makeText(getApplicationContext(), "Max video duration", Toast.LENGTH_LONG).show();
            }

            binding.recordBtn.setImageResource(R.drawable.ic_record_pressed);
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            customHandler.removeCallbacks(updateTimer);

            File videoFile = cameraSupport.stopRecord();
            if  (totalVideoDuration <= MAX_VIDEO_DURATION_MILLISEC && videoFile != null) {
                timeSwapBuff += segmentDurationInMillisec;

                videoSegments.add(new VideoSegment(videoFile, (int)segmentDurationInMillisec));

            }


            binding.recordBtn.setImageResource(R.drawable.ic_record);
            Log.d(TAG, "totalVideoDuration: " + totalVideoDuration + "\nsegmentDurationInMillisec: " + segmentDurationInMillisec + "\n timeSwapBuff: " + timeSwapBuff);
            return true;
        }
        return false;
    }


    private void initBtns() {
        binding.exitBtn.setOnClickListener(v -> finish());
        binding.recordBtn.setOnTouchListener((v, event) -> onRecordPressed(event.getAction()));
        binding.cameraBtn.setOnClickListener(v -> onPhotoPressed());
        binding.flashBtn.setOnClickListener(v -> onFlashPressed());
        binding.deleteBtn.setOnClickListener(v -> {
        });//TODO delete previous video fragment and seconds of record
        binding.confirmBtn.setOnClickListener(v -> {
        });//TODO make united video file from fragments
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

    private void onPhotoPressed() {
        File path = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString(), getString(R.string.app_name) + "/");
        path.mkdirs();
        String fileName = new SimpleDateFormat("'photo_'yMMdd_HHmmss'.jpg'", Locale.getDefault()).format(new Date());
        File photoFile = new File(path, fileName);
        cameraSupport.takePicture(photoFile);
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



}
