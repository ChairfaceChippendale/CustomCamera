package com.softensy.customcamera;

import android.databinding.DataBindingUtil;
import android.support.annotation.DrawableRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.softensy.customcamera.cameraUtil.CameraController;
import com.softensy.customcamera.cameraUtil.CameraControllerImpl;
import com.softensy.customcamera.cameraUtil.CameraOld;
import com.softensy.customcamera.databinding.ActivityCameraBinding;

public class CameraActivity extends AppCompatActivity implements CameraControllerImpl.Listener {

    private static final String TAG = CameraActivity.class.getSimpleName();

    ActivityCameraBinding binding;

    CameraController cameraController;

    boolean flashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera);

        cameraController = new CameraControllerImpl(
                new CameraOld(binding.cameraView.getHolder()),
                this,
                2000,
                10000,
                this
        );

        initBtns();
    }

    private void initBtns() {
        binding.exitBtn.setOnClickListener(v -> finish());
        binding.recordBtn.setOnTouchListener((v, event) -> onRecordPressed(event.getAction()));
        binding.cameraBtn.setOnClickListener(v -> cameraController.makePhoto());
        binding.flashBtn.setOnClickListener(v -> onFlashPressed());
        binding.deleteBtn.setOnClickListener(v -> cameraController.deleteLastVideoSegment());
        binding.confirmBtn.setOnClickListener(v -> cameraController.mergeVideo());
        binding.delayBtn.setOnClickListener(v -> cameraController.delayRec());
        binding.stopAutoRecBtn.setOnClickListener(v -> cameraController.stopAutoRec());
    }


    private boolean onRecordPressed(int action) {
        if (action == MotionEvent.ACTION_DOWN) {
            cameraController.runRec();
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            cameraController.pauseRec();
            return true;
        }
        return false;
    }


    @Override
    protected void onResume() {
        super.onResume();
        cameraController.prepareCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraController.releaseCamera();
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
    public void showProgressView(boolean show) {
        binding.progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void updateTimeline(int time, @DrawableRes int color) {
        binding.timeline.setProgress(time);
        binding.timeline.setProgressDrawable(getResources().getDrawable(color));
    }

    @Override
    public void setRecBtn(@DrawableRes int icon) {
        binding.recordBtn.setImageResource(icon);
    }

    @Override
    public void setStopAutoRecBtnVisibility(boolean visibility) {
        binding.stopAutoRecBtn.setVisibility(visibility ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void setInterfaceVisibility(boolean visibility) {
        final int v = visibility ? View.VISIBLE : View.INVISIBLE;
        binding.exitBtn.setVisibility(v);
        binding.flashBtn.setVisibility(v);
        binding.cameraBtn.setVisibility(v);
        binding.deleteBtn.setVisibility(v);
        binding.confirmBtn.setVisibility(v);
        binding.delayBtn.setVisibility(v);
    }

    @Override
    public void setRecBtnVisibility(boolean visibility) {
        binding.recordBtn.setVisibility(visibility ? View.VISIBLE : View.INVISIBLE);
    }
}
