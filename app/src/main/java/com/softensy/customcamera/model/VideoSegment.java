package com.softensy.customcamera.model;


import java.io.File;

public class VideoSegment {

    private File videoFile;
    private Integer duration;

    public File getVideoFile() {
        return videoFile;
    }

    public Integer getDuration() {
        return duration;
    }

    public VideoSegment(File videoFile, Integer duration) {
        this.videoFile = videoFile;
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "file: " + getVideoFile().getAbsolutePath() +
                "\nduration: " + getDuration();
    }
}
