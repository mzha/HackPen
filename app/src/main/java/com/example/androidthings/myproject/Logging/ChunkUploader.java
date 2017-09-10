package com.example.androidthings.myproject.Logging;

import android.app.DownloadManager;
import android.util.Log;

import com.example.androidthings.myproject.Chunk;
import com.example.androidthings.myproject.MainActivity;
import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Future;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Arthur on 9/9/17.
 * Allocates new chunks as quickly as possible while reasonable
 */

public class ChunkUploader implements Runnable {

    private ArrayList<Chunk> data;
    private boolean stopped = false;
    private int index;

    public ChunkUploader(ArrayList<Chunk> data) {
        this.data = data;
        index = 0;
    }

    @Override
    public void run() {
        // Always run in background priority.  If main thread ever catches up to this, we can simply
        // increase chunk size
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        while (!stopped) {
            // if this chunk is full
            if (data.size() > 0 && data.get(index).timestamps[MainActivity.POLLS_PER_SECOND-1] > 0) {
                try {
                    OkHttpClient client = new OkHttpClient();

                    MediaType mediaType = MediaType.parse("application/json");
                    RequestBody body = RequestBody.create(mediaType, "{\n\t\"timestamps\": " + Arrays.toString(data.get(index).timestamps) +
                            ",\n\t\"xs\": " + Arrays.toString(data.get(index).xs) +
                            ",\n\t\"ys\": " + Arrays.toString(data.get(index).ys) +
                            ",\n\t\"zs\": " + Arrays.toString(data.get(index).zs) + "\n}");
                    Request request = new Request.Builder()
                            .url("https://hackpen-179409.appspot.com/api/uploadChunk")
                            .post(body)
                            .addHeader("content-type", "application/json")
                            .addHeader("cache-control", "no-cache")
                            .addHeader("postman-token", "29690783-ed02-06ad-b892-a56d7332851e")
                            .build();

                    Response response = client.newCall(request).execute();
                } catch (Exception e) {
                }
                index++;

            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stop() {
        stopped = true;
    }
}
