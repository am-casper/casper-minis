package com.example.casperminis;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSink;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;


public class MainActivity extends AppCompatActivity {
    private ExoPlayer exoPlayer;
    private DefaultTrackSelector trackSelector = null;
    private Cache downloadCache = null;
    private String DOWNLOAD_CONTENT_DIRECTORY = "downloads";
    private float volume = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getData();
    }
    @Override
    protected void onStop() {
        super.onStop();
        exoPlayer.setPlayWhenReady(false);
    }
    private void getData() {
        final RequestQueue requestQueue = Volley.newRequestQueue(this);
        final String url = "http://fatema.takatakind.com/app_api/index.php?p=showAllVideos";
        final StringRequest mStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                JSONObject res;
                try {
                    res = new JSONObject(response);
                    JSONArray vidArray = res.getJSONArray("msg");
                    playVideo(vidArray, 0);
                    setContent(vidArray, 0);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                displayErrorWidgets();
            }
        });
        requestQueue.add(mStringRequest);
    }

    private void displayErrorWidgets() {
        Toast.makeText(MainActivity.this,"NO INTERNET CONNECTIVITY",Toast.LENGTH_SHORT).show();
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        final ImageView reload = findViewById(R.id.reload);
        reload.setVisibility(View.VISIBLE);
        reload.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                getData();
                reload.setVisibility(View.GONE);
            }
        });
    }

    private void setContent(JSONArray vidArray, int i) {
        final TextView channel = findViewById(R.id.channel);
        final TextView caption = findViewById(R.id.caption);
        final TextView likesCount = findViewById(R.id.likes);
        final TextView commentsCount = findViewById(R.id.comments);
        final TextView viewsCount = findViewById(R.id.views);
        final TextView handle = findViewById(R.id.handle);
        final ImageView dp = findViewById(R.id.dp);
        try {
            JSONObject videoJson = vidArray.getJSONObject(i);
            final JSONObject userInfo = videoJson.getJSONObject("user_info");
            String imageUrl = userInfo.getString("profile_pic");
            Log.d("Image URL", "setContent: "+imageUrl);
            Glide.with(this)
                    .load(imageUrl)
                    .apply(new RequestOptions())
                    .into(dp);
            channel.setText(String.format("%s %s", userInfo.getString("first_name"), userInfo.getString("last_name")));
            caption.setText(videoJson.getString("description"));
            handle.setText(userInfo.getString("username"));
            final JSONObject countInfo = videoJson.getJSONObject("count");
            likesCount.setText(countInfo.getString("like_count"));
            commentsCount.setText(countInfo.getString("video_comment_count"));
            viewsCount.setText(countInfo.getString("view"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    private void playVideo(JSONArray vidArray, int i) {
        try {


            ConstraintLayout layout = findViewById(R.id.layout);
            StyledPlayerView styledPlayerView = findViewById(R.id.videoView);
            JSONObject videoJson = vidArray.getJSONObject(i);
            final ProgressBar progressBar = findViewById(R.id.progressBar);
            String url = videoJson.getString("video");
            exoPlayer = new ExoPlayer.Builder(MainActivity.this).build();
            styledPlayerView.setPlayer(exoPlayer);
            exoPlayer.addListener(new ExoPlayer.Listener() {
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    if (playbackState == ExoPlayer.STATE_BUFFERING){
                        progressBar.setVisibility(View.VISIBLE);
                    } else {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }
            });
            exoPlayer.setVolume(volume);
            if(isNetworkAvailable()) {
                    MediaItem mediaItem = MediaItem.fromUri(url);
                    ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(buildCacheDataSourceFactory()).createMediaSource(mediaItem);
                    exoPlayer.setMediaSource(mediaSource);
                    exoPlayer.prepare();
                exoPlayer.setPlayWhenReady(true);
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            }
            else {
                displayErrorWidgets();
                return;
            }
            layout.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
                @Override
                public void onClick() {
                    super.onClick();
                    exoPlayer.setVolume(exoPlayer.getVolume()>0?0:1);
                    volume = exoPlayer.getVolume();
                    if (volume >0)
                        Toast.makeText(MainActivity.this, "Un-muted", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(MainActivity.this, "Muted", Toast.LENGTH_SHORT).show();

                }
                @Override
                public void onSwipeUp() {
                    super.onSwipeUp();
                    exoPlayer.release();
                    playVideo(vidArray, i+1);
                    setContent(vidArray, i+1);
                }
                @Override
                public void onSwipeDown() {
                    super.onSwipeDown();
                    if (i != 0) {
                        exoPlayer.release();
                        playVideo(vidArray, i-1);
                        setContent(vidArray, i-1);
                    } else {
                        Toast.makeText(MainActivity.this, "You are at the very beginning. Swipe up to start your journey", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            final ImageView reload = findViewById(R.id.reload);
            reload.setVisibility(View.GONE);
        } catch (Exception e) {
            displayErrorWidgets();
        }

    }

    private CacheDataSource.Factory buildCacheDataSourceFactory() {
        System.out.print("hmmmmm");
        Cache cache = getDownloadCache();
        CacheDataSink.Factory cacheSink = new CacheDataSink.Factory().setCache(cache);
        DefaultDataSource.Factory upstreamFactory = new DefaultDataSource.Factory(this, new DefaultHttpDataSource.Factory());
        return new CacheDataSource.Factory()
                .setCache(cache)
                .setCacheWriteDataSinkFactory(cacheSink)
                .setCacheReadDataSourceFactory(new FileDataSource.Factory())
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    private Cache getDownloadCache() {
        if (downloadCache == null) {
            File downloadContentDirectory = new File(
                    getExternalFilesDir(null),
                    DOWNLOAD_CONTENT_DIRECTORY
            );
            downloadCache =
                    new SimpleCache(downloadContentDirectory,
                            new NoOpCacheEvictor(),
                            new StandaloneDatabaseProvider(this));
        }
        return downloadCache;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

}