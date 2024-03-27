package com.example.casperminis;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getData(0);
    }
    private void getData(int index) {
        final RequestQueue requestQueue = Volley.newRequestQueue(this);
        final String url = "http://fatema.takatakind.com/app_api/index.php?p=showAllVideos";
        final String[] data = new String[10];
        final StringRequest mStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                JSONObject res;
                try {
                    res = new JSONObject(response);
                    JSONArray vidArray = res.getJSONArray("msg");
                    playVideo(vidArray, index);
                    setContent(vidArray, index);
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
                getData(0);
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
            dp.setImageURI(Uri.parse("http://res.cloudinary.com/cinespace/image/upload/v1693680378/samples/dessert-on-a-plate.jpg"));
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
            JSONObject videoJson = vidArray.getJSONObject(i);
            final ProgressBar progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
            String url = videoJson.getString("video");
            final VideoView videoView = findViewById(R.id.videoView);
            final TextView handle = findViewById(R.id.handle);
            if (i == 0) videoView.setVideoPath(url);
            if (isNetworkAvailable()) videoView.start();
            else displayErrorWidgets();
            videoView.setVideoPath(vidArray.getJSONObject(i+1).getString("video"));
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    progressBar.setVisibility(View.GONE);
                    final ImageView reload = findViewById(R.id.reload);
                    reload.setVisibility(View.GONE);
                    mp.setLooping(true);
                }
            });
//            videoView.set
            videoView.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
                public void onSwipeTop() {
                    videoView.suspend();
                    playVideo(vidArray, i+1);
                    setContent(vidArray, i+1);
                }
                public void onSwipeBottom() {
                    if (i != 0) {
                        videoView.suspend();
                        playVideo(vidArray, i-1);
                        setContent(vidArray, i-1);
                    } else {
                        Toast.makeText(MainActivity.this, "You are at the very beginning. Swipe up to start your journey", Toast.LENGTH_SHORT).show();
                    }
                }

            });

            if (!videoView.isPlaying()) {
                videoView.start();
            }
        } catch (Exception e) {
            displayErrorWidgets();
        }

    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

}