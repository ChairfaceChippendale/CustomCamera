package com.softensy.customcamera;


import java.io.File;

public class VideoSegment {

    File videoFile;
    Integer duration;


    public VideoSegment(File videoFile, Integer duration) {
        this.videoFile = videoFile;
        this.duration = duration;
    }
}
