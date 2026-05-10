package com.example.myapp.presenter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;

import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.myapp.model.VideoItem;

/**
 * Leanback card presenter for VideoItem objects.
 *
 * FIX: RequestOptions built once and reused — creating new RequestOptions per bind
 *      caused memory pressure leading to OOM crashes on low-RAM Android 9 TV devices.
 * FIX: Glide.with(context) not Glide.with(activity) — activity may be null during
 *      recycling on API 28 Leanback, causing IllegalArgumentException.
 */
public class VideoCardPresenter extends Presenter {

    private static final int W = 320, H = 180;

    private static final int[] COLORS = {
        0xFF1A1A2E, 0xFF16213E, 0xFF0F3460,
        0xFF533483, 0xFF2B2D42, 0xFF1B262C
    };
    private int mColorIdx = 0;

    // FIX: single reusable RequestOptions instance
    private static final RequestOptions GLIDE_OPTS = new RequestOptions()
        .centerCrop()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .override(W, H);

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        ImageCardView card = new ImageCardView(parent.getContext());
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setMainImageDimensions(W, H);
        card.setInfoAreaBackgroundColor(0xFF1C1C1C);
        return new ViewHolder(card);
    }

    @Override
    public void onBindViewHolder(ViewHolder vh, Object item) {
        VideoItem video = (VideoItem) item;
        ImageCardView card = (ImageCardView) vh.view;
        // FIX: use card.getContext() (always non-null) not activity
        Context ctx = card.getContext().getApplicationContext();

        card.setTitleText(video.getTitle());
        String sub = video.getChannelName();
        if (!video.getDuration().isEmpty()) sub += "  •  " + video.getDuration();
        card.setContentText(sub);
        card.setMainImageDimensions(W, H);

        int placeholder = COLORS[mColorIdx++ % COLORS.length];
        String url = video.getThumbnailUrl();

        if (url != null && !url.isEmpty()) {
            Glide.with(ctx)
                 .load(url)
                 .apply(GLIDE_OPTS)
                 .placeholder(new ColorDrawable(placeholder))
                 .error(new ColorDrawable(placeholder))
                 .into(card.getMainImageView());
        } else {
            card.setMainImage(new ColorDrawable(placeholder));
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder vh) {
        ImageCardView card = (ImageCardView) vh.view;
        // FIX: cancel Glide load on unbind to avoid recycled-view crash on API 28
        Glide.with(card.getContext().getApplicationContext()).clear(card.getMainImageView());
        card.setMainImage(null);
    }
}
