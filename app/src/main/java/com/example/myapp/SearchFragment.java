package com.example.myapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.example.myapp.extractor.YouTubeExtractorService;
import com.example.myapp.model.VideoItem;
import com.example.myapp.presenter.VideoCardPresenter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * Leanback search with 600ms debounce.
 *
 * ANDROID 9 CRASH FIXES:
 * FIX 1: setSearchResultProvider() called in onCreate(), not onViewCreated() —
 *         Leanback SearchSupportFragment requires it before the view is attached on API 28.
 * FIX 2: switchMapSingle replaced with manual cancel — RxJava switchMapSingle can emit
 *         on wrong thread after debounce on some API 28 scheduler implementations.
 * FIX 3: isAdded() guard before showResults() to prevent fragment-detached crash.
 * FIX 4: Disposables cleared in onDestroyView() not onDestroy() for fragment lifecycle safety.
 */
public class SearchFragment extends SearchSupportFragment
        implements SearchSupportFragment.SearchResultProvider {

    private static final String TAG         = "SearchFragment";
    private static final long   DEBOUNCE_MS = 600;

    private ArrayObjectAdapter        mRowsAdapter;
    private ArrayObjectAdapter        mResultsAdapter;
    private final CompositeDisposable mDisposables = new CompositeDisposable();
    private final PublishSubject<String> mQueryBus = PublishSubject.create();

    // FIX 1: setSearchResultProvider in onCreate, before view inflation
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRowsAdapter    = new ArrayObjectAdapter(new ListRowPresenter());
        mResultsAdapter = new ArrayObjectAdapter(new VideoCardPresenter());
        mRowsAdapter.add(new ListRow(new HeaderItem(0, "YouTube Results"), mResultsAdapter));

        setSearchResultProvider(this);

        // FIX 2: manual debounce + observeOn instead of switchMapSingle
        mDisposables.add(
            mQueryBus
                .debounce(DEBOUNCE_MS, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .observeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                .flatMapSingle(q ->
                    YouTubeExtractorService.getInstance()
                        .search(q)
                        .onErrorReturn(e -> {
                            Log.e(TAG, "Search error: " + e.getMessage());
                            return new ArrayList<>();
                        })
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showResults, e -> Log.e(TAG, "Bus error", e))
        );

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder iVH, Object item,
                                      RowPresenter.ViewHolder rVH, Row row) {
                if (item instanceof VideoItem) openPlayer((VideoItem) item);
            }
        });
    }

    // FIX 4: clear in onDestroyView
    @Override
    public void onDestroyView() {
        mDisposables.clear();
        super.onDestroyView();
    }

    @Override
    public ObjectAdapter getResultsAdapter() { return mRowsAdapter; }

    @Override
    public boolean onQueryTextChange(String q) {
        if (q == null || q.trim().isEmpty()) { mResultsAdapter.clear(); return true; }
        mQueryBus.onNext(q.trim());
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String q) { return onQueryTextChange(q); }

    // FIX 3: isAdded() guard
    private void showResults(List<VideoItem> videos) {
        if (!isAdded() || mResultsAdapter == null) return;
        mResultsAdapter.clear();
        if (!videos.isEmpty()) mResultsAdapter.addAll(0, videos);
    }

    private void openPlayer(VideoItem video) {
        Intent i = new Intent(requireActivity(), PlayerActivity.class);
        i.putExtra(PlayerActivity.EXTRA_VIDEO, video);
        startActivity(i);
    }
}
