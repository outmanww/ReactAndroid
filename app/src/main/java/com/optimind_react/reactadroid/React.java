package com.optimind_react.reactadroid;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Warren on 2016/03/21.
 */
public class React extends Application {
    private String email;
    private String password;
    private String apiToken;
    @Override
    public void onCreate()
    {
        super.onCreate();

        // Get from the SharedPreferences
        SharedPreferences settings = getApplicationContext().getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        email = settings.getString(getString(R.string.pref_key_email), null);
        password = settings.getString(getString(R.string.pref_key_password), null);
        apiToken = settings.getString(getString(R.string.pref_key_api_token), null);
    }

    public String getApiToken(){
        return this.apiToken;
    }
    public void setApiToken(String value)
    {
        this.apiToken = value;

        // Save data to the SharedPreferences
        SharedPreferences settings = getApplicationContext().getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(getString(R.string.pref_key_api_token), this.apiToken);
        editor.apply();
    }
    public String getEmail(){ return this.email; }
    public void setEmail(String value)
    {
        this.email = value;
        // Save data to the SharedPreferences
        SharedPreferences settings = getApplicationContext().getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(getString(R.string.pref_key_email), this.email);
        editor.apply();
    }
    public String getPassword(){
        return this.password;
    }
    public void setPassword(String value)
    {
        this.password = value;
        // Save data to the SharedPreferences
        SharedPreferences settings = getApplicationContext().getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(getString(R.string.pref_key_password), this.password);
        editor.apply();
    }
}
