package com.optimind_react.reactadroid;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class React extends Application
{
    private String mApiToken;
    private String mEmail;
    private String mName;
    private boolean mScreenStatus = true;

    // for detect foreground or back ground
    private AppStatus mAppStatus = AppStatus.FOREGROUND;
    @Override
    public void onCreate()
    {
        super.onCreate();

        // Get from the SharedPreferences
        SharedPreferences settings = getApplicationContext().getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        mApiToken = settings.getString(getString(R.string.pref_key_api_token), null);

        // register application activity life cycle call back
        registerActivityLifecycleCallbacks(new ReactActivityLifecycleCallbacks());

        IntentFilter regFilter = new IntentFilter();
        // get device shutdown or reboot event
        regFilter.addAction(Intent.ACTION_SCREEN_ON);
        regFilter.addAction(Intent.ACTION_SCREEN_OFF);
        regFilter.addAction(Intent.ACTION_REBOOT);
        regFilter.addAction(Intent.ACTION_SHUTDOWN);
        registerReceiver(receiver, regFilter );
    }

    // screen on <-> screen off
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(final Context context, final Intent intent) {
            //check if the broadcast is our desired one
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
                mScreenStatus = false;
            else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
                mScreenStatus = true;
        }
    };

    // get current name
    public boolean getScreenStatus()
    {
        return mScreenStatus;
    }

    // get current name
    public String getName()
    {
        return this.mName;
    }

    // set new name
    public void setName(String value)
    {
        this.mName = value;

        // Save data to the SharedPreferences
        SharedPreferences settings = getApplicationContext().getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(getString(R.string.pref_key_username), this.mName);
        editor.apply();
    }

    // get current api token
    public String getApiToken()
    {
        return this.mApiToken;
    }

    // set new api token
    public void setApiToken(String value)
    {
        this.mApiToken = value;

        // Save data to the SharedPreferences
        SharedPreferences settings = getApplicationContext().getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(getString(R.string.pref_key_api_token), this.mApiToken);
        editor.apply();
    }

    // get current email
    public String getEmail()
    {
        return this.mEmail;
    }

    // set new email
    public void setEmail(String value)
    {
        this.mEmail = value;

        // Save data to the SharedPreferences
        SharedPreferences settings = getApplicationContext().getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(getString(R.string.pref_key_email), this.mApiToken);
        editor.apply();
    }

    // get current status
    public AppStatus getAppStatus()
    {
        return mAppStatus;
    }

    // check if app is foreground
    public boolean isForeground()
    {
        return mAppStatus.ordinal() > AppStatus.BACKGROUND.ordinal();
    }

    public enum AppStatus
    {
        BACKGROUND,                      // app is background
        RETURNED_TO_FOREGROUND,         // app returned to foreground(or first launch)
        FOREGROUND                       // app is foreground
    }

    public class ReactActivityLifecycleCallbacks implements ActivityLifecycleCallbacks
    {
        // running activity count
        private int running = 0;

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {}

        @Override
        public void onActivityResumed(Activity activity) {}

        @Override
        public void onActivityPaused(Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}

        @Override
        public void onActivityDestroyed(Activity activity) {}

        @Override
        public void onActivityStarted(Activity activity)
        {
            running++;
            if (running == 1)
            {
                // running activity is 1, app must be returned from background just now (or first launch)
                mAppStatus = AppStatus.RETURNED_TO_FOREGROUND;
            }
            else if (running > 1)
            {
                // 2 or more running activities should be foreground already.
                mAppStatus = AppStatus.FOREGROUND;
            }
        }

        @Override
        public void onActivityStopped(Activity activity)
        {
            running--;
            if (running == 0)
            {
                // no active activity
                // app goes to background
                mAppStatus = AppStatus.BACKGROUND;
            }
        }
    }

    public static boolean isEmailValid(String email)
    {
        boolean isValid = false;

        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";

        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        if (matcher.matches())
            isValid = true;
        return isValid;
    }

    public static boolean isPasswordValid(String password)
    {
        boolean isValid = true;

        if(password.length()<6)
            isValid = false;

        String expression = "^[a-zA-Z0-9]+$";
        if(!password.matches(expression))
            isValid = false;
        return isValid;
    }
}