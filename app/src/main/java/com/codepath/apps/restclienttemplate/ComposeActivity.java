package com.codepath.apps.restclienttemplate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.codepath.apps.restclienttemplate.models.MainUser;
import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;

import org.json.JSONException;
import org.parceler.Parcels;

import okhttp3.Headers;

public class ComposeActivity extends AppCompatActivity {

    public static final String TAG = "ComposeActivity";
    public static final int MAX_TWEET_LENGTH = 280;

    ImageView ivUserImage;
    TextView tvUserName;
    TextView tvUserHandle;

    EditText etCompose;
    Button btnTweet;
    TextView counter;

    TwitterClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        client = TwitterApp.getRestClient(this);

        etCompose = findViewById(R.id.etCompose);
        btnTweet = findViewById(R.id.btnTweet);
        counter = findViewById(R.id.tvCounter);

        MainUser mainUser = Parcels.unwrap(getIntent().getParcelableExtra("mainUser"));
        drawMainUser(mainUser);

        // Initialize counter with 0 chars.
        counter.setText(String.format("%d/%d", 0, MAX_TWEET_LENGTH));

        // Set click listener on button.
        btnTweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tweetContent = etCompose.getText().toString();
                if (tweetContent.isEmpty()) {
                    Toast.makeText(ComposeActivity.this, "Tweet cannot be empty.", Toast.LENGTH_SHORT).show();
                    return;
                } else if (tweetContent.length() > MAX_TWEET_LENGTH) {
                    Toast.makeText(ComposeActivity.this, "Character Limit Exceeded.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(ComposeActivity.this, tweetContent, Toast.LENGTH_SHORT).show();
                // Make an API call to publish tweet
                client.publishTweet(tweetContent, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Headers headers, JSON json) {
                        Log.i(TAG, "onSuccess to publish tweet");
                        try {
                            Tweet tweet = Tweet.fromJsonTrunct(json.jsonObject);
                            Log.i(TAG, "Published tweet: " + tweet);
                            Intent intent = new Intent();
                            intent.putExtra("tweet", Parcels.wrap(tweet));
                            // Sets result code and bundle data for response.
                            setResult(RESULT_OK, intent);
                            // Closes the activity, passes data to parent.
                            finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                        Log.e(TAG, "onFailure to publish tweet", throwable);
                    }
                });
            }
        });

        etCompose.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                counter.setText(String.format("%d/%d", charSequence.length(), MAX_TWEET_LENGTH));
                if (charSequence.length() == 0) {
                    counter.setTextColor(Color.GRAY);
                } else if (charSequence.length() <= MAX_TWEET_LENGTH) {
                    counter.setTextColor(Color.GREEN);
                } else {
                    counter.setTextColor(Color.RED);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        // Make an API call to Twitter to publish the tweet.
    }

    public void drawMainUser(MainUser mainUser) {
        ivUserImage = findViewById(R.id.ivUserImageComp);
        tvUserName = findViewById(R.id.tvUserNameComp);
        tvUserHandle = findViewById(R.id.tvUserHandleComp);

        tvUserName.setText(mainUser.userName);
        tvUserHandle.setText(String.format("@%s", mainUser.userHandle));
        Glide.with(getApplicationContext())
                .load(mainUser.userImageUrl)
                .circleCrop()
                .into(ivUserImage);
        Log.i(TAG, "onUserDrawn");
    }
}