package com.example.myapp.model;

/**
 * Holds resolved stream URLs.
 * audioUrl is null  → videoUrl is a muxed progressive stream (use setMediaItem).
 * audioUrl non-null → videoUrl is video-only; merge with audioUrl via MergingMediaSource.
 */
public class StreamResult {
    public final String videoUrl;
    public final String audioUrl;

    public StreamResult(String videoUrl, String audioUrl) {
        this.videoUrl = videoUrl;
        this.audioUrl = audioUrl;
    }
}