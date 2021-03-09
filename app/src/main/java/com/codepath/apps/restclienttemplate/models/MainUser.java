package com.codepath.apps.restclienttemplate.models;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcel;

@Parcel
public class MainUser {

    public String userName;
    public String userHandle;
    public String userImageUrl;

    // Empty constructor for Parceler library.
    public MainUser() {};

    public static MainUser fromUserDataJson(JSONObject userObject) throws JSONException {
        MainUser mainUser = new MainUser();
        mainUser.userName = userObject.getString("name");
        mainUser.userHandle = userObject.getString("screen_name");

        mainUser.userImageUrl = userObject.getString("profile_image_url_https");

        return mainUser;
    }



}
