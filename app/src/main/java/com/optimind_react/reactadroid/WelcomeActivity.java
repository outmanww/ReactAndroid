package com.optimind_react.reactadroid;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class WelcomeActivity extends Activity
{
    // tag for debug
    private final static String TAG = WelcomeActivity.class.getSimpleName();

    // next activity
    private final int ACTIVITY_MAIN = 0;
    private final int ACTIVITY_LOGIN = 1;
    private int nextActivity = ACTIVITY_MAIN;

    // transition timer handler
    private final Handler mTransHandler = new Handler();
    // timer run function
    private final Runnable autoTransition = new Runnable()
    {
        @SuppressLint("InlinedApi")
        @Override
        public void run()
        {
            Intent intent;
            if(nextActivity == ACTIVITY_LOGIN)
                intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            else
                intent = new Intent(WelcomeActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // get saved api token
        React mApp = (React) this.getApplication();
        String apiToken = mApp.getApiToken();
        if(apiToken == null)
        {
            nextActivity = ACTIVITY_LOGIN;
        }
        else
        {
            // verify the api token
            final String url = getString(R.string.domain)+"/student/apitoken?api_token="+apiToken;
            ConfirmApiTokenTask confirmTokenTask = new ConfirmApiTokenTask(url);
            confirmTokenTask.execute();
        }
        mTransHandler.postDelayed(autoTransition, getResources().getInteger(R.integer.timer_welcome));
    }

    /**
     * Represents an api token confirm task used to authenticate
     * the user.
     */
    private class ConfirmApiTokenTask extends AsyncTask<Void,Integer,Integer>
    {
        private final String mUrl;

        public ConfirmApiTokenTask(String url)
        {
            super();
            mUrl = url;
        }

        @Override
        protected Integer doInBackground(Void...voids)
        {
            Log.d(TAG, "ConfirmApiTokenTask Start");
            HttpURLConnection con = null;
            URL url;
            Integer responseCode = 400;

            try
            {
                url = new URL(mUrl);
                con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("GET");
                con.setInstanceFollowRedirects(false);
                con.setRequestProperty("Content-Length", "0");
                con.setRequestProperty("Accept-Language", "ja");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Content-Type", "application/json");
                con.setUseCaches(false);
                con.setAllowUserInteraction(false);
                con.setConnectTimeout(getResources().getInteger(R.integer.delay_http_connect));
                con.setReadTimeout(getResources().getInteger(R.integer.delay_http_read));
                con.setDoInput(true);

                con.connect();
                responseCode = con.getResponseCode();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                if( con != null )
                    con.disconnect();
            }
            return responseCode;
        }

        @Override
        protected void onPostExecute(Integer responseCode)
        {
            Log.d(TAG, "ConfirmApiTokenTask Finish");
            if(HttpURLConnection.HTTP_OK != responseCode)
                nextActivity = ACTIVITY_LOGIN;
        }
    }
}