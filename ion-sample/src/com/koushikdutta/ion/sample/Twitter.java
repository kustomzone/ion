package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

public class Twitter extends Activity {
    // adapter that holds tweets, obviously :)
    ArrayAdapter<JsonObject> tweetAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable global Ion logging
        Ion.getDefault(this).setLogging("ion-sample", Log.DEBUG);

        // create a tweet adapter for our list view
        tweetAdapter = new ArrayAdapter<JsonObject>(this, 0) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null)
                    convertView = getLayoutInflater().inflate(R.layout.tweet, null);

                // we're near the end of the list adapter, so load more items
                if (position >= getCount() - 3)
                    load();

                // grab the tweet (or retweet)
                JsonObject tweet = getItem(position);
                JsonObject retweet = tweet.getAsJsonObject("retweeted_status");
                if (retweet != null)
                    tweet = retweet;

                // grab the user info... name, profile picture, tweet text
                JsonObject user = tweet.getAsJsonObject("user");
                String twitterId = user.get("screen_name").getAsString();

                // set the profile photo using Ion
                String imageUrl = String.format("https://api.twitter.com/1/users/profile_image?screen_name=%s&size=bigger", twitterId);

                ImageView imageView = (ImageView)convertView.findViewById(R.id.image);

                // Use Ion's builder set the google_image on an ImageView from a URL

                // start with the ImageView
                Ion.with(imageView)
                    // use a placeholder google_image if it needs to load from the network
                    .placeholder(R.drawable.twitter)
                    // use a fade in animation when it finishes loading
                    .animateIn(AnimationUtils.loadAnimation(getContext(), R.anim.fadein))
                    // load the url
                    .load(imageUrl);


                // and finally, set the name and text
                TextView handle = (TextView)convertView.findViewById(R.id.handle);
                handle.setText(twitterId);

                TextView text = (TextView)convertView.findViewById(R.id.tweet);
                text.setText(tweet.get("text").getAsString());
                return convertView;
            }
        };

        // basic setup of the ListView and adapter
        setContentView(R.layout.twitter);
        ListView listView = (ListView)findViewById(R.id.list);
        listView.setAdapter(tweetAdapter);

        // do the first load
        load();
    }

    // This "Future" tracks loading operations.
    // A Future is an object that manages the state of an operation
    // in progress that will have a "Future" result.
    // You can attach callbacks (setCallback) for when the result is ready,
    // or cancel() it if you no longer need the result.
    Future<JsonArray> loading;

    private void load() {
        // don't attempt to load more if a load is already in progress
        if (loading != null && !loading.isDone() && !loading.isCancelled())
            return;

        // load the tweets
        String url = "https://api.twitter.com/1/statuses/user_timeline.json?include_entities=true&include_rts=true&screen_name=koush&count=20";
        if (tweetAdapter.getCount() > 0) {
            // load from the "last" id
            JsonObject last = tweetAdapter.getItem(tweetAdapter.getCount() - 1);
            url += "&max_id=" + last.get("id_str").getAsString();
        }

        // Request tweets from Twitter using Ion.
        // This is done using Ion's Fluent/Builder API.
        // This API lets you chain calls together to build
        // complex requests.

        // This request loads a URL as JsonArray and invokes
        // a callback on completion.
        loading = Ion.with(this, url)
            .asJsonArray()
            .setCallback(new FutureCallback<JsonArray>() {
                @Override
                public void onCompleted(Exception e, JsonArray result) {
                    // this is called back onto the ui thread, no Activity.runOnUiThread or Handler.post necessary.
                    if (e != null) {
                        Toast.makeText(Twitter.this, "Error loading tweets", Toast.LENGTH_LONG).show();
                        return;
                    }
                    // add the tweets
                    for (int i = 0; i < result.size(); i++) {
                        tweetAdapter.add(result.get(i).getAsJsonObject());
                    }
                }
            });
    }
}
