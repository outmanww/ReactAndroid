package com.optimind_react.reactadroid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class WelcomeActivity extends Activity{
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
//    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
//    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int TRANSITION_DELAY = 1000;
    private final Handler mTransHandler = new Handler();
//    private View mContentView;
    private WelcomeActivity selfClass = this;


    // next activity
    public static final int ACTIVITY_ROOM_ENTER = 0;
    public static final int ACTIVITY_LOGIN = 1;
    private int nextActivity = ACTIVITY_ROOM_ENTER;

    private final Runnable autoTransition = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            Intent intent;
            if(nextActivity == ACTIVITY_LOGIN)
            {
                intent = new Intent(selfClass, LoginActivity.class);
            }
            else
            {
                intent = new Intent(selfClass, RoomEnterActivity.class);
            }
            startActivity(intent);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            finish();
        }
    };

    public void httpRequestFinished(Integer status, JSONObject data)
    {

    };
    /*
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    */
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    /*
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);

//        mVisible = true;
//        mControlsView = findViewById(R.id.fullscreen_content_controls);
//        mContentView = findViewById(R.id.fullscreen_content);

        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
/*        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
*/
        // Set up the user interaction to manually show or hide the system UI.
        /*
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
        */

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        // findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        // get saved email and password
        React mApp = (React) this.getApplication();
        String apiToken = mApp.getApiToken();
//        apiToken = "61327787d059cc097f9e306d2c531d4f";
        if(apiToken == null)
        {
            nextActivity = ACTIVITY_LOGIN;
        }
        else
        {
            // verify the api token
//            final String url = getString(R.string.domain)+"/student/rooms/001700?api_token="+apiToken;
            final String url = getString(R.string.domain)+"/student/apitoken?api_token="+apiToken;
            AsyncHttpRequest confirmTokenTask = new AsyncHttpRequest(url,"GET");
            confirmTokenTask.execute();
        }
        mTransHandler.postDelayed(autoTransition, TRANSITION_DELAY);
    }

    private class AsyncHttpRequest extends AsyncTask<Void,Integer,Integer>
    {
        private String mUrl;
        private String mMethod;
//    private final static String EOL = "\r\n";

        public AsyncHttpRequest(String url, String method)
        {
            super();
            mUrl = url;
            mMethod = method;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // doInBackground前処理
        }

        @Override
        protected Integer doInBackground(Void...voids)
        {
            // httpのコネクションを管理するクラス
            HttpURLConnection con = null;
            URL url = null;
            int status = 0;

            try {
                // URLの作成
                url = new URL(mUrl);
                // 接続用HttpURLConnectionオブジェクト作成
                con = (HttpURLConnection)url.openConnection();
                // リクエストメソッドの設定
                con.setRequestMethod(mMethod);
                // リダイレクトを自動で許可しない設定
                con.setInstanceFollowRedirects(false);
                con.setRequestProperty("Content-length", "0");
                con.setRequestProperty("Accept-Language", "jp");
                con.setUseCaches(false);
                con.setAllowUserInteraction(false);
                con.setConnectTimeout(1000);
                con.setReadTimeout(1000);
                con.connect();
                return con.getResponseCode();
                /*
                switch (status) {
                    case HttpURLConnection.HTTP_OK:
                    case 201:
                        try
                        {
                            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                sb.append(line+"\n");
                            }
                            br.close();

                            JSONObject jsonObj = new JSONObject(sb.toString());
                            return jsonObj;
                        }
                        catch (Exception eJson)
                        {
                            eJson.printStackTrace();
                        }
                }
                */
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // setProgressPercent(progress[0]);
        }

        @Override
        protected void onPostExecute(Integer status)
        {
            if(HttpURLConnection.HTTP_OK != status)
                nextActivity = ACTIVITY_LOGIN;
        }
    }

/*
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }
*/
    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    /*
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
    */
}
