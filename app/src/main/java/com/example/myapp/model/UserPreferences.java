package com.example.myapp.model;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UserPreferences {

    private static final String PREF_FILE     = "ayoutube_prefs";
    private static final String KEY_INTERESTS = "interests";
    private static final String KEY_HISTORY   = "watch_history";

    private static final Set<String> DEFAULT_INTERESTS =
        new HashSet<>(Arrays.asList("Trending", "Music", "Gaming"));

    private final SharedPreferences mPrefs;

    public UserPreferences(Context ctx) {
        // FIX: use applicationContext to avoid Activity leak on API 28
        mPrefs = ctx.getApplicationContext()
                    .getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    public Set<String> getInterests() {
        // FIX: copy the set — SharedPreferences returns an unmodifiable set on API 28
        Set<String> stored = mPrefs.getStringSet(KEY_INTERESTS, null);
        return stored != null ? new HashSet<>(stored) : new HashSet<>(DEFAULT_INTERESTS);
    }

    public void recordWatched(String videoId) {
        if (videoId == null || videoId.isEmpty()) return;
        Set<String> history = new HashSet<>(getWatchHistory());
        history.add(videoId);
        mPrefs.edit().putStringSet(KEY_HISTORY, history).apply();
    }

    public Set<String> getWatchHistory() {
        Set<String> s = mPrefs.getStringSet(KEY_HISTORY, null);
        return s != null ? new HashSet<>(s) : new HashSet<>();
    }

    public static String interestToQuery(String interest) {
        if (interest == null) return "trending videos";
        switch (interest) {
            case "Music":     return "best music videos 2024";
            case "Gaming":    return "gaming highlights 2024";
            case "News":      return "world news today";
            case "Sports":    return "sports highlights 2024";
            case "Comedy":    return "best comedy videos";
            case "Education": return "educational videos";
            case "Tech":      return "technology news 2024";
            default:          return "trending videos";
        }
    }
}
