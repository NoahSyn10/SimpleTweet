package com.codepath.apps.restclienttemplate;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.job.JobInfo;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.codepath.apps.restclienttemplate.models.MainUser;
import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.apps.restclienttemplate.models.User;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;

public class TimelineActivity extends AppCompatActivity {

    public static final String TAG = "TimelineActivity";
    private final int REQUEST_CODE = 20;

    MainUser mainUser;

    ImageView ivUserImage;
    TextView tvUserName;
    TextView tvUserHandle;

    TwitterClient client;
    RecyclerView rvTweets;
    List<Tweet> tweets;
    TweetsAdapter tweetsAdapter;
    SwipeRefreshLayout swipeContainer;
    EndlessRecyclerViewScrollListener scrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        client = TwitterApp.getRestClient(this);

        swipeContainer = findViewById(R.id.swipeContainer);
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i(TAG, "fetching new data");
                populateHomeTimeline();
            }
        });

        // Find the recycler view
        rvTweets = findViewById(R.id.rvTweets);
        // Init the list of tweets and adapter
        tweets = new ArrayList<>();
        tweetsAdapter = new TweetsAdapter(this, tweets);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        // Recyclerview setup: layout manager and the adapter
        rvTweets.setLayoutManager(layoutManager);
        rvTweets.setAdapter(tweetsAdapter);

        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                Log.i(TAG, "onLoadMore" + page);
                loadMoreData();
            }
        };
        // Adds scroll listener to recyclerview
        rvTweets.addOnScrollListener(scrollListener);

        populateUserSection();
        populateHomeTimeline();
    }

    private void loadMoreData() {
        // Send an api request to retrieve appropriate paginated data.
        client.getNextPageOfTweets(new JsonHttpResponseHandler() {
           @Override
           public void onSuccess(int statusCode, Headers headers, JSON json) {
                Log.i(TAG, "onSuccess for 'loadMoreData" + json.toString());
                // Deserialize and construct new model objects from API response.
                JSONArray jsonArray = json.jsonArray;
               try {
                   List<Tweet> tweets = Tweet.fromJsonArray(jsonArray);
                   // Append new data to the existing set of items.
                   // Notify the adapter.
                   tweetsAdapter.addAll(tweets);
               } catch (JSONException e) {
                   e.printStackTrace();
               }
           }

           @Override
           public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
               Log.i(TAG, "onFailure for 'loadMoreData' " + throwable);
               Toast.makeText(getApplicationContext(), "Rate Limit Exceeded", Toast.LENGTH_LONG).show();
           }
        }, tweets.get(tweets.size()-1).id);
    }

    private void populateHomeTimeline() {
        client.getHomeTimeline(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                Log.i(TAG, "onSuccess" + json.toString());
                JSONArray jsonArray = json.jsonArray;
                try {
                    tweetsAdapter.clear();
                    tweetsAdapter.addAll(Tweet.fromJsonArray(jsonArray));
                    swipeContainer.setRefreshing(false);
                    Log.i(TAG, "onAddAllSuccess");
                } catch (JSONException e) {
                    Log.e(TAG, "Json exception");
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onApiError" + response, throwable);
                try {
                    Log.i(TAG, "onErrorToast");
                    JSONObject responseJson = new JSONObject(response);
                    Toast.makeText(getApplicationContext(), responseJson.getJSONArray("errors").getJSONObject(1).getString("message"), Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    Log.e(TAG, "Error reading response", e);
                    e.printStackTrace();
                }

            }
        });
    }

    private void populateUserSection() {
        // Get user settings
        client.getUserCreds(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                Log.i(TAG, "onSuccessUserCreds");
                try {
                    long userId = json.jsonObject.getLong("id");

                    // Get User Data after getting id
                    client.getUser(new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Headers headers, JSON json) {
                            Log.i(TAG, "onSuccessMainUser");
                            JSONObject userObject = json.jsonObject;
                            try {
                                mainUser = MainUser.fromUserDataJson(userObject);
                                drawMainUser(mainUser);
                            } catch (JSONException e) {
                                Log.e(TAG, "onFailureMainUserCreation");
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                            Log.e(TAG, "onFailureMainUser " + response);
                        }
                    }, userId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailureUserCreds");
            }
        });
    }

    public void drawMainUser(MainUser mainUser) {
        ivUserImage = findViewById(R.id.ivUserImage);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserHandle = findViewById(R.id.tvUserHandle);

        tvUserName.setText(mainUser.userName);
        tvUserHandle.setText(String.format("@%s", mainUser.userHandle));
        Glide.with(getApplicationContext())
                .load(mainUser.userImageUrl)
                .circleCrop()
                .into(ivUserImage);
        Log.i(TAG, "onUserDrawn");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu to add item to action bar.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.compose) {
            // Compose icon is tapped
            Log.i(TAG, "onComposeClick");
            // Navigate to the compose activity
            Intent intent = new Intent(this, ComposeActivity.class);
            intent.putExtra("mainUser", Parcels.wrap(mainUser));
            startActivityForResult(intent, REQUEST_CODE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            // Get data from the intent
            Tweet tweet = Parcels.unwrap(data.getParcelableExtra("tweet"));
            // Update recyclerview with new tweet
            // Modify data source
            tweets.add(0, tweet);
            // Update adapter
            tweetsAdapter.notifyItemInserted(0);
            rvTweets.smoothScrollToPosition(0);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}