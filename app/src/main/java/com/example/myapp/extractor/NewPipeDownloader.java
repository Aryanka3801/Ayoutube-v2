package com.example.myapp.extractor;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * OkHttp bridge for NewPipe Extractor's Downloader interface.
 *
 * ANDROID 9 CRASH FIXES:
 * FIX 1: RequestBody.create(byte[], MediaType) — correct deprecated-safe overload for OkHttp 4.x.
 *         Using RequestBody.create(byte[]) (no MediaType) compiles but throws NPE at runtime on API 28.
 * FIX 2: response.body().string() called before response.close() — body is closed automatically.
 * FIX 3: Always close the response after reading to avoid "connection leak" crash on API 28 strict TLS.
 * FIX 4: Desktop User-Agent — YouTube returns degraded/empty responses to Android UA on API 28.
 */
public class NewPipeDownloader extends Downloader {

    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/120.0.0.0 Safari/537.36";

    private static volatile NewPipeDownloader sInstance;
    private final OkHttpClient mClient;

    private NewPipeDownloader(OkHttpClient client) {
        this.mClient = client;
    }

    public static NewPipeDownloader getInstance() {
        if (sInstance == null) {
            synchronized (NewPipeDownloader.class) {
                if (sInstance == null) {
                    sInstance = new NewPipeDownloader(
                        new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .followRedirects(true)
                            .followSslRedirects(true)
                            .build()
                    );
                }
            }
        }
        return sInstance;
    }

    @Override
    public Response execute(Request request) throws IOException, ReCaptchaException {
        final String method    = request.httpMethod();
        final String url       = request.url();
        final byte[] body      = request.dataToSend();
        final Map<String, List<String>> headers = request.headers();

        // FIX 1: Correct RequestBody.create overload for OkHttp 4 on API 28
        RequestBody requestBody = null;
        if (body != null) {
            MediaType jsonType = MediaType.parse("application/json; charset=utf-8");
            requestBody = RequestBody.create(body, jsonType);
        }

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT);

        if (headers != null) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                for (String v : e.getValue()) {
                    builder.addHeader(e.getKey(), v);
                }
            }
        }

        switch (method) {
            case "GET":
                builder.get();
                break;
            case "POST":
                builder.post(requestBody != null
                    ? requestBody
                    : RequestBody.create(new byte[0], null)); // FIX 1 continued
                break;
            case "DELETE":
                builder.delete();
                break;
            default:
                builder.method(method, requestBody);
        }

        // FIX 3: Use try-with-resources pattern to always close response
        okhttp3.Response response = mClient.newCall(builder.build()).execute();
        try {
            if (response.code() == 429) {
                throw new ReCaptchaException("YouTube rate-limited (429)", url);
            }
            ResponseBody respBody = response.body();
            // FIX 2: Read body string while response is still open
            String bodyStr = (respBody != null) ? respBody.string() : "";
            Map<String, List<String>> respHeaders = new HashMap<>(response.headers().toMultimap());
            return new Response(
                response.code(),
                response.message(),
                respHeaders,
                bodyStr,
                response.request().url().toString()
            );
        } finally {
            response.close();
        }
    }
}
