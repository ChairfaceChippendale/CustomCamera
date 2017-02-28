package com.softensy.customcamera;

import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.googlecode.mp4parser.BasicContainer;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.softensy.customcamera.cameraUtil.CameraOld;
import com.softensy.customcamera.cameraUtil.CameraSupport;
import com.softensy.customcamera.databinding.ActivityCameraBinding;
import com.softensy.customcamera.model.VideoSegment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;


//http://stackoverflow.com/questions/26035547/how-to-pause-resume-the-video-recording

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = CameraActivity.class.getSimpleName();

    ActivityCameraBinding binding;

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

            if (totalVideoDuration > MIN_VIDEO_DURATION_MILLISEC && timelineIsRed) {
                //"timelineIsRed" prevents changing of timeline color too often
                binding.timeline.setProgressDrawable(getResources().getDrawable(R.drawable.progress_green));
                timelineIsRed = false;
            }

            if (totalVideoDuration >= MAX_VIDEO_DURATION_MILLISEC) {
                customHandler.removeCallbacks(updateTimer);
                File videoFile = cameraSupport.stopRecord();
                if (videoFile != null) {
                    timeSwapBuff += segmentDurationInMillisec;
                    videoSegments.addLast(new VideoSegment(videoFile, (int) segmentDurationInMillisec));
                }
                binding.recordBtn.setImageResource(R.drawable.ic_record);
                Log.d(TAG, "totalVideoDuration: " + totalVideoDuration + "\nsegmentDurationInMillisec: " + segmentDurationInMillisec + "\n timeSwapBuff: " + timeSwapBuff);
                if (binding.stopAutoRecBtn.isShown()) {
                    binding.stopAutoRecBtn.setVisibility(View.GONE);
                    binding.cameraBtn.setVisibility(View.VISIBLE);
                    binding.deleteBtn.setVisibility(View.VISIBLE);
                    binding.confirmBtn.setVisibility(View.VISIBLE);
                    binding.recordBtn.setVisibility(View.VISIBLE);
                    binding.delayBtn.setVisibility(View.VISIBLE);
                }
            } else {
                customHandler.postDelayed(this, STEP_VIDEO_DURATION_MILLISEC);
            }
        }
    };

    CameraSupport cameraSupport;
    Deque<VideoSegment> videoSegments;

    boolean flashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera);
        cameraSupport = new CameraOld(binding.cameraView.getHolder());
        videoSegments = new ArrayDeque<>();
        initBtns();
    }

    private void initBtns() {
        binding.exitBtn.setOnClickListener(v -> finish());
        binding.recordBtn.setOnTouchListener((v, event) -> onRecordPressed(event.getAction()));
        binding.cameraBtn.setOnClickListener(v -> onPhotoPressed());
        binding.flashBtn.setOnClickListener(v -> onFlashPressed());
        binding.deleteBtn.setOnClickListener(v -> deleteLastVideoSegment());
        binding.confirmBtn.setOnClickListener(v -> {
            if (totalVideoDuration > MIN_VIDEO_DURATION_MILLISEC) {
                new MergeVideo(videoSegments).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                Toast.makeText(this, "Too shot", Toast.LENGTH_SHORT).show();
            }
        });
        binding.delayBtn.setOnClickListener(v -> {
            if (totalVideoDuration <= MAX_VIDEO_DURATION_MILLISEC) {
                binding.cameraBtn.setVisibility(View.GONE);
                binding.recordBtn.setVisibility(View.GONE);
                binding.deleteBtn.setVisibility(View.GONE);
                binding.confirmBtn.setVisibility(View.GONE);
                binding.delayBtn.setVisibility(View.GONE);
                new Handler().postDelayed(
                        () -> {
                            cameraSupport.startRecord(provideFileToRecord());
                            startHTime = SystemClock.uptimeMillis();
                            customHandler.postDelayed(updateTimer, 0);
                            binding.stopAutoRecBtn.setVisibility(View.VISIBLE);
                        },
                        3000);
            }
        });
        binding.stopAutoRecBtn.setOnClickListener(v -> {
            customHandler.removeCallbacks(updateTimer);

            File videoFile = cameraSupport.stopRecord();
            if (totalVideoDuration <= MAX_VIDEO_DURATION_MILLISEC && videoFile != null) {
                timeSwapBuff += segmentDurationInMillisec;
                videoSegments.addLast(new VideoSegment(videoFile, (int) segmentDurationInMillisec));
            }

            binding.stopAutoRecBtn.setVisibility(View.GONE);
            binding.cameraBtn.setVisibility(View.VISIBLE);
            binding.deleteBtn.setVisibility(View.VISIBLE);
            binding.confirmBtn.setVisibility(View.VISIBLE);
            binding.recordBtn.setVisibility(View.VISIBLE);
            binding.delayBtn.setVisibility(View.VISIBLE);

        });

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
            if (totalVideoDuration <= MAX_VIDEO_DURATION_MILLISEC && videoFile != null) {
                timeSwapBuff += segmentDurationInMillisec;
                videoSegments.addLast(new VideoSegment(videoFile, (int) segmentDurationInMillisec));
                Log.d(TAG, "videoSegments added");
            }

            binding.recordBtn.setImageResource(R.drawable.ic_record);
//            Log.d(TAG, "totalVideoDuration: " + totalVideoDuration + "\nsegmentDurationInMillisec: " + segmentDurationInMillisec + "\n timeSwapBuff: " + timeSwapBuff);
            return true;
        }
        return false;
    }




    private void deleteLastVideoSegment() {
        if (!videoSegments.isEmpty()) {
            VideoSegment toDel = videoSegments.pollLast();
            totalVideoDuration -= toDel.getDuration();
            timeSwapBuff -= toDel.getDuration();
            if (totalVideoDuration < MIN_VIDEO_DURATION_MILLISEC) {
                binding.timeline.setProgressDrawable(getResources().getDrawable(R.drawable.progress_red));
                timelineIsRed = true;
            }
            binding.timeline.setProgress((int) totalVideoDuration);
            toDel.getVideoFile().delete();
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



    File provideFileToRecord() {

        String path = getString(R.string.app_name) + "/temp/";

        File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString(),
                path);

        dir.mkdirs();

        String fileName = new SimpleDateFormat("'temp_video_'yMMdd_HHmmss'.mp4'", Locale.getDefault()).format(new Date());
        return new File(dir, fileName);
    }


    public class MergeVideo extends AsyncTask<Void, Void, Void> {

        List<VideoSegment> segments = new ArrayList<>();

        public MergeVideo(Deque<VideoSegment> segments) {
            this.segments.addAll(segments);
        }

        @Override
        protected void onPreExecute() {
            binding.progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            int count = segments.size();
            try {
                String paths[] = new String[count];
                Movie[] inMovies = new Movie[count];
                for (int i = 0; i < count; i++) {
                    paths[i] = segments.get(i).getVideoFile().getAbsolutePath();
                    inMovies[i] = MovieCreator.build(new FileDataSourceImpl(paths[i]));
                }
                List<Track> videoTracks = new LinkedList<>();
                List<Track> audioTracks = new LinkedList<>();
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

                String dirName = "/" + getString(R.string.app_name) + "/";
                String fileName = new SimpleDateFormat("'video_'yMMdd_HHmmss'.mp4'", Locale.getDefault()).format(new Date());

                @SuppressWarnings("resource")
                FileChannel fc = new RandomAccessFile(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + dirName + fileName,
                        "rw"
                ).getChannel();
                out.writeContainer(fc);
                fc.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void value) {
            super.onPostExecute(value);
            binding.progress.setVisibility(View.GONE);

            for (VideoSegment vc: videoSegments) {
                vc.getVideoFile().delete();
            }
            videoSegments.clear();
            totalVideoDuration = 0L;
            timeSwapBuff = 0L;
            binding.timeline.setProgress((int)totalVideoDuration);
            binding.timeline.setProgressDrawable(getResources().getDrawable(R.drawable.progress_red));
            timelineIsRed = true;
        }
    }


}
