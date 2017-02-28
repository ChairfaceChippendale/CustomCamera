package com.softensy.customcamera.cameraUtil;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.DrawableRes;
import android.util.Log;

import com.googlecode.mp4parser.BasicContainer;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.softensy.customcamera.R;
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

public class CameraControllerImpl implements CameraController {

    private static final String TAG = CameraControllerImpl.class.getSimpleName();

    private static final int STEP_VIDEO_DURATION_MILLISEC = 100;
    private static final int AUTO_REC_DELAY = 3000;

    private CameraSupport camera;
    private Listener listener;
    private int minVideoDurationMillisec;
    private int maxVideoDurationMillisec;

    private Deque<VideoSegment> videoSegments;

    private Context context;

    public CameraControllerImpl(CameraSupport camera, Listener listener, int minVideoDurationMillisec, int maxVideoDurationMillisec, Context context) {
        this.camera = camera;
        this.listener = listener;
        this.minVideoDurationMillisec = minVideoDurationMillisec;
        this.maxVideoDurationMillisec = maxVideoDurationMillisec;
        this.context = context;
    }

    private long totalVideoDuration = 0L;
    private long startHTime = 0L;
    private long segmentDurationInMillisec = 0L;
    private long timeSwapBuff = 0L;
    private Handler customHandler = new Handler();
    private Runnable updateTimer = new Runnable() {
        public void run() {
            segmentDurationInMillisec = SystemClock.uptimeMillis() - startHTime;
            totalVideoDuration = timeSwapBuff + segmentDurationInMillisec;

            if (totalVideoDuration > minVideoDurationMillisec) {
                listener.updateTimeline((int) totalVideoDuration, R.drawable.progress_green);
            } else {
                listener.updateTimeline((int) totalVideoDuration, R.drawable.progress_red);
            }

            if (totalVideoDuration >= maxVideoDurationMillisec) {
                customHandler.removeCallbacks(updateTimer);
                File videoFile = camera.stopRecord();
                if (videoFile != null) {
                    timeSwapBuff += segmentDurationInMillisec;
                    videoSegments.addLast(new VideoSegment(videoFile, (int) segmentDurationInMillisec));
                }
                listener.setRecBtn(R.drawable.ic_record_pause);
                listener.setInterfaceVisibility(true);
                listener.setRecBtnVisibility(true);
                listener.setStopAutoRecBtnVisibility(false);
            } else {
                customHandler.postDelayed(this, STEP_VIDEO_DURATION_MILLISEC);
            }
        }
    };

    @Override
    public void delayRec() {
        if (totalVideoDuration <= maxVideoDurationMillisec) {
            listener.setInterfaceVisibility(false);
            listener.setRecBtnVisibility(false);
            new Handler().postDelayed(
                    () -> {
                        camera.startRecord(provideFileToRecord());
                        startHTime = SystemClock.uptimeMillis();
                        customHandler.postDelayed(updateTimer, 0);
                        listener.setStopAutoRecBtnVisibility(true);
                    },
                    AUTO_REC_DELAY);
        }
    }

    @Override
    public void stopAutoRec() {
        customHandler.removeCallbacks(updateTimer);
        File videoFile = camera.stopRecord();
        if (totalVideoDuration <= maxVideoDurationMillisec && videoFile != null) {
            timeSwapBuff += segmentDurationInMillisec;
            addNewSegment(new VideoSegment(videoFile, (int) segmentDurationInMillisec));
        }
        listener.setInterfaceVisibility(true);
        listener.setRecBtnVisibility(true);
        listener.setStopAutoRecBtnVisibility(false);
    }


    @Override
    public void runRec() {
        if (totalVideoDuration <= maxVideoDurationMillisec) {
            camera.startRecord(provideFileToRecord());
            startHTime = SystemClock.uptimeMillis();
            customHandler.postDelayed(updateTimer, 0);
        } else {
            listener.showToast("Max video duration");
        }
        listener.setRecBtn(R.drawable.ic_record_run);
        listener.setInterfaceVisibility(false);
    }

    @Override
    public void pauseRec() {

        customHandler.removeCallbacks(updateTimer);
        File videoFile = camera.stopRecord();
        if (totalVideoDuration <= maxVideoDurationMillisec && videoFile != null) {
            timeSwapBuff += segmentDurationInMillisec;
            addNewSegment(new VideoSegment(videoFile, (int) segmentDurationInMillisec));
            Log.d(TAG, "videoSegments added");
        }
        listener.setRecBtn(R.drawable.ic_record_pause);
        listener.setInterfaceVisibility(true);
    }

    @Override
    public void makePhoto() {
        File path = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString(), context.getString(R.string.app_name) + "/");
        path.mkdirs();
        String fileName = new SimpleDateFormat("'photo_'yMMdd_HHmmss'.jpg'", Locale.getDefault()).format(new Date());
        File photoFile = new File(path, fileName);
        camera.takePicture(photoFile);
    }

    @Override
    public void prepareCamera() {
        camera.prepareCamera();
    }

    @Override
    public void releaseCamera() {
        camera.releaseCamera();
    }

    @Override
    public void deleteLastVideoSegment() {
        if (!videoSegments.isEmpty()) {
            VideoSegment toDel = videoSegments.pollLast();
            totalVideoDuration -= toDel.getDuration();
            timeSwapBuff -= toDel.getDuration();
            if (totalVideoDuration < minVideoDurationMillisec) {
                listener.updateTimeline((int) totalVideoDuration, R.drawable.progress_red);
            } else {
                listener.updateTimeline((int) totalVideoDuration, R.drawable.progress_green);
            }
            toDel.getVideoFile().delete();
        }
    }


    private void addNewSegment(VideoSegment segment) {
        if (videoSegments == null) {
            videoSegments = new ArrayDeque<>();
        }
        videoSegments.add(segment);
    }

    private File provideFileToRecord() {

        String path = context.getString(R.string.app_name) + "/temp/";

        File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString(),
                path);

        dir.mkdirs();

        String fileName = new SimpleDateFormat("'temp_video_'yMMdd_HHmmss'.mp4'", Locale.getDefault()).format(new Date());
        return new File(dir, fileName);
    }

    @Override
    public void mergeVideo() {
        if (totalVideoDuration > minVideoDurationMillisec) {
            new MergeVideo(videoSegments).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            listener.showToast("Too shot");
        }
    }


    private class MergeVideo extends AsyncTask<Void, Void, Void> {

        List<VideoSegment> segments = new ArrayList<>();

        MergeVideo(Deque<VideoSegment> segments) {
            this.segments.addAll(segments);
        }

        @Override
        protected void onPreExecute() {
            listener.showProgressView(true);
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

                String dirName = "/" + context.getString(R.string.app_name) + "/";
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

            listener.showProgressView(false);

            for (VideoSegment vc : videoSegments) {
                vc.getVideoFile().delete();
            }
            videoSegments.clear();
            totalVideoDuration = 0L;
            timeSwapBuff = 0L;

            listener.updateTimeline((int) totalVideoDuration, R.drawable.progress_red);
        }
    }


    public interface Listener {

        void updateTimeline(int progress, @DrawableRes int timelineColor);

        void setInterfaceVisibility(boolean visibility);

        void setStopAutoRecBtnVisibility (boolean visibility);

        void setRecBtnVisibility (boolean visibility);

        void setRecBtn(@DrawableRes int icon);

        void showProgressView(boolean show);

        void showToast(String text);

    }

}
