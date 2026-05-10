package com.example.myapp;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MergingMediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.extractor.DefaultExtractorsFactory;
import com.example.myapp.model.StreamResult;
import androidx.media3.ui.PlayerView;

import com.example.myapp.extractor.YouTubeExtractorService;
import com.example.myapp.model.VideoItem;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import okhttp3.OkHttpClient;

/**
 * Full-screen ExoPlayer activity.
 *
 * ANDROID 9 CRASH FIXES:
 * FIX 1: No @OptIn(UnstableApi) class annotation — this annotation is processed differently
 *         by the Kotlin compiler and causes NoSuchMethodError on API 28 pure-Java builds.
 *         Media3 1.2.1 stable APIs used throughout — no @UnstableApi methods needed.
 * FIX 2: Legacy setSystemUiVisibility() for fullscreen — WindowInsetsController is API 30+.
 * FIX 3: Player released in onDestroy() only after mPlayer null check; Surface leak prevented
 *         by calling mPlayer.clearVideoSurface() before release on API 28.
 * FIX 4: OkHttpDataSource.Factory(callFactory) constructor — the .create() static helper
 *         is not available in media3-datasource-okhttp 1.2.1 on API 28.
 * FIX 5: ExoPlayer.Builder(context) — uses activity context, not applicationContext,
 *         to get correct Display metrics for surface allocation on API 28.
 * FIX 6: getSerializableExtra(key) without Class<T> arg — the 2-arg version is API 33+;
 *         using deprecated 1-arg version with explicit cast is correct for API 28.
 */
@SuppressWarnings("deprecation")   // setSystemUiVisibility + getSerializableExtra on API 28
public class PlayerActivity extends FragmentActivity {

    public static final String EXTRA_VIDEO = "extra_video";
    private static final String TAG        = "PlayerActivity";

    private PlayerView  mPlayerView;
    private View        mLoadingOverlay;
    private View        mErrorOverlay;
    private TextView    mErrorText;
    private Button      mRetryBtn;

    private OkHttpDataSource.Factory mHttpFactory;
    private ExoPlayer   mPlayer;
    private VideoItem   mVideo;
    private boolean     mPlayerReady = false;

    private final CompositeDisposable mDisposables = new CompositeDisposable();

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FIX 2: Legacy fullscreen flags — WindowInsetsController is API 30+ only
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );

        setContentView(R.layout.activity_player);
        bindViews();

        // FIX 6: 1-arg getSerializableExtra + cast — API 28 compatible
        Object extra = getIntent().getSerializableExtra(EXTRA_VIDEO);
        if (!(extra instanceof VideoItem)) { finish(); return; }
        mVideo = (VideoItem) extra;

        mRetryBtn.setOnClickListener(v -> {
            mErrorOverlay.setVisibility(View.GONE);
            loadAndPlay();
        });

        buildPlayer();
        loadAndPlay();
    }

    @Override
    protected void onStart()   { super.onStart(); if (mPlayer != null) mPlayer.play(); }
    @Override
    protected void onStop()    { super.onStop();  if (mPlayer != null) mPlayer.pause(); }

    @Override
    protected void onDestroy() {
        mDisposables.clear();
        releasePlayer();
        super.onDestroy();
    }

    // ── Player construction ───────────────────────────────────────────────

    private void buildPlayer() {
        // FIX 4: OkHttpDataSource.Factory(OkHttpClient) constructor for media3 1.2.1
        OkHttpClient okHttp = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

        mHttpFactory = new OkHttpDataSource.Factory(okHttp);
mHttpFactory.setUserAgent(
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
);

        // FIX 5: activity context (this) not getApplicationContext()
        mPlayer = new ExoPlayer.Builder(this)
    .setMediaSourceFactory(new DefaultMediaSourceFactory(mHttpFactory))
    .build();

        mPlayerView.setPlayer(mPlayer);
        mPlayerView.setUseController(true);
        mPlayerView.setControllerAutoShow(true);
        mPlayerView.setControllerShowTimeoutMs(3000);

        mPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                mLoadingOverlay.setVisibility(
                    state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                if (state == Player.STATE_READY) {
                    mPlayerReady = true;
                    mErrorOverlay.setVisibility(View.GONE);
                }
            }
            @Override
            public void onPlayerError(PlaybackException e) {
                Log.e(TAG, "Playback error", e);
                mLoadingOverlay.setVisibility(View.GONE);
                mErrorText.setText(getString(R.string.error_load) + "\n" +
                    (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                mErrorOverlay.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadAndPlay() {
        mLoadingOverlay.setVisibility(View.VISIBLE);
        mErrorOverlay.setVisibility(View.GONE);

        mDisposables.add(
            YouTubeExtractorService.getInstance()
    .getStreamResult(mVideo.getVideoId())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    this::startPlayback,
                    err -> {
                        Log.e(TAG, "Extraction error", err);
                        mLoadingOverlay.setVisibility(View.GONE);
                        mErrorText.setText(getString(R.string.error_load) + "\n" +
                            (err.getMessage() != null ? err.getMessage() : "Extraction failed"));
                        mErrorOverlay.setVisibility(View.VISIBLE);
                    }
                )
        );
    }

    private void startPlayback(StreamResult result) {
    if (mPlayer == null) return;
    Log.d(TAG, "Playing: " + result.videoUrl.substring(0, Math.min(60, result.videoUrl.length())));

    if (result.audioUrl != null) {
        // Video-only stream: merge video + audio tracks
        DefaultExtractorsFactory ef = new DefaultExtractorsFactory();
        ProgressiveMediaSource videoSource = new ProgressiveMediaSource.Factory(mHttpFactory, ef)
            .createMediaSource(MediaItem.fromUri(result.videoUrl));
        ProgressiveMediaSource audioSource = new ProgressiveMediaSource.Factory(mHttpFactory, ef)
            .createMediaSource(MediaItem.fromUri(result.audioUrl));
        mPlayer.setMediaSource(new MergingMediaSource(videoSource, audioSource));
    } else {
        // Muxed stream: simple play
        mPlayer.setMediaItem(MediaItem.fromUri(result.videoUrl));
    }
    mPlayer.prepare();
    mPlayer.setPlayWhenReady(true);
}

    /** FIX 3: null check + clearVideoSurface() before release on API 28 */
    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.clearVideoSurface();
            mPlayer.release();
            mPlayer = null;
        }
    }

    // ── Views ─────────────────────────────────────────────────────────────

    private void bindViews() {
        mPlayerView     = findViewById(R.id.player_view);
        mLoadingOverlay = findViewById(R.id.loading_overlay);
        mErrorOverlay   = findViewById(R.id.error_overlay);
        mErrorText      = findViewById(R.id.error_text);
        mRetryBtn       = findViewById(R.id.retry_button);
    }

    // ── D-pad / Remote ────────────────────────────────────────────────────

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE) {
            finish(); return true;
        }
        if (mPlayer == null) return super.onKeyDown(code, e);
        switch (code) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (mPlayer.isPlaying()) mPlayer.pause(); else mPlayer.play(); return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mPlayer.seekTo(mPlayer.getCurrentPosition() + 10_000); return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mPlayer.seekTo(Math.max(0, mPlayer.getCurrentPosition() - 10_000)); return true;
        }
        return super.onKeyDown(code, e);
    }
}
