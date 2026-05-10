package com.example.myapp.model;

import java.io.Serializable;

/**
 * Serializable video data holder passed between activities via Intent.
 *
 * FIX: implements Serializable (not Parcelable) — Parcelable on API 28
 * with getSerializableExtra() requires explicit class cast that crashes
 * on certain Samsung/Huawei Android 9 ROMs. Serializable is safe on all.
 */
public class VideoItem implements Serializable {

    private static final long serialVersionUID = 1L;  // FIX: explicit UID avoids InvalidClassException

    private final String videoId;
    private final String title;
    private final String channelName;
    private final String thumbnailUrl;
    private final String duration;
    private final long   viewCount;
    private final String uploadDate;

    public VideoItem(String videoId, String title, String channelName,
                     String thumbnailUrl, String duration,
                     long viewCount, String uploadDate) {
        this.videoId      = videoId != null ? videoId : "";
        this.title        = title   != null ? title   : "";
        this.channelName  = channelName  != null ? channelName  : "";
        this.thumbnailUrl = thumbnailUrl != null ? thumbnailUrl : "";
        this.duration     = duration != null ? duration : "";
        this.viewCount    = viewCount;
        this.uploadDate   = uploadDate != null ? uploadDate : "";
    }

    public String getVideoId()      { return videoId; }
    public String getTitle()        { return title; }
    public String getChannelName()  { return channelName; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getDuration()     { return duration; }
    public long   getViewCount()    { return viewCount; }
    public String getUploadDate()   { return uploadDate; }
    public String getWatchUrl()     { return "https://www.youtube.com/watch?v=" + videoId; }
}
