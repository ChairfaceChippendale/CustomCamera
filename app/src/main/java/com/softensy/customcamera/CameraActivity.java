package com.softensy.customcamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.googlecode.mp4parser.BasicContainer;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.softensy.customcamera.databinding.ActivityCameraBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;


//http://stackoverflow.com/questions/26035547/how-to-pause-resume-the-video-recording

public class CameraActivity extends AppCompatActivity {


    private static final int PERMISSIONS_REQUEST = 12345;
    ActivityCameraBinding binding;

    Camera camera;
    MediaRecorder mediaRecorder;

    File path;
    File videoFile;

    boolean flashOn = false;

    Handler recordHandler = new Handler();
    Runnable runRecord = () -> {
        if (prepareVideoRecorder()) {
            mediaRecorder.start();
        } else {
            releaseMediaRecorder();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera);

        initBtns();
    }

    private void initBtns() {
        binding.exitBtn.setOnClickListener(v -> finish());

        binding.recordBtn.setOnTouchListener((v, event) -> onRecordPressed(event));
        binding.cameraBtn.setOnClickListener(v -> onCameraPressed());
        binding.flashBtn.setOnClickListener(v -> onFlashPressed());
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


    public boolean onRecordPressed(MotionEvent event) {

        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            binding.recordBtn.setImageResource(R.drawable.ic_record_pressed);
            recordHandler.postDelayed(runRecord, 500);
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            recordHandler.removeCallbacks(runRecord);
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                releaseMediaRecorder();
            }
            binding.recordBtn.setImageResource(R.drawable.ic_record);
            return true;
        }
        return false;
    }

    public void onCameraPressed() {
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
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
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



    public class MergeVideo extends AsyncTask<String, Integer, String> {

        int count;
        String filename;


        @Override
        protected void onPreExecute() {

        };

        @Override
        protected String doInBackground(String... params) {
            try {
                String paths[] = new String[count];
                Movie[] inMovies = new Movie[count];
                for (int i = 0; i < count; i++) {
                    paths[i] = path + filename + String.valueOf(i + 1) + ".mp4";
                    inMovies[i] = MovieCreator.build(new FileInputStream(paths[i]).getChannel());
                }
                List<Track> videoTracks = new LinkedList<Track>();
                List<Track> audioTracks = new LinkedList<Track>();
                for (Movie m : inMovies) {
                    for (Track t : m.getTracks()) {
                        if (t.getHandler().equals("soun")) {
                            audioTracks.add(t);
                        }
                        if (t.getHandler().equals("vide")) {
                            videoTracks.add(t);
                        }
                    }
                }

                Movie result = new Movie();

                if (audioTracks.size() > 0) {
                    result.addTrack(new AppendTrack(audioTracks
                            .toArray(new Track[audioTracks.size()])));
                }
                if (videoTracks.size() > 0) {
                    result.addTrack(new AppendTrack(videoTracks
                            .toArray(new Track[videoTracks.size()])));
                }

                BasicContainer out = (BasicContainer) new DefaultMp4Builder()
                        .build(result);

                @SuppressWarnings("resource")
                FileChannel fc = new RandomAccessFile(String.format(Environment
                        .getExternalStorageDirectory() + "/wishbyvideo.mp4"),
                        "rw").getChannel();
                out.writeContainer(fc);
                fc.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            String mFileName = Environment.getExternalStorageDirectory()
                    .getAbsolutePath();
            mFileName += "/wishbyvideo.mp4";
            filename = mFileName;
            return mFileName;
        }

        @Override
        protected void onPostExecute(String value) {
            super.onPostExecute(value);



        }
    }
}
