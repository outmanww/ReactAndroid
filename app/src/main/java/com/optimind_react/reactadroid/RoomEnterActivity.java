package com.optimind_react.reactadroid;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

public class RoomEnterActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener
{
    // tag for debug
    private final static String TAG = RoomEnterActivity.class.getSimpleName();

//    private LocationManager mLocationManager;
    GoogleApiClient mGoogleApiClient;
    Location mLocation;
    private InputMethodManager mInputMethodManager;

    private RoomConfirmTask mRoomConfirmTask = null;
    private RoomInTask mRoomInTask = null;

    // UI references.
    private EditText mRoomKeyView;
    private View mProgressView;
    private View mKeyAreaView;
    private LinearLayout mRootLayout;


    // http result
    private String mTeacherName;
    private String mLectureName;
    private String mTimeSlot;
    private String mRoomKey;
    private String mApiToken;

    private void registerLocationUpdates()
    {
        // if the last location is got in one minute
        if(mLocation != null && mLocation.getTime() > Calendar.getInstance().getTimeInMillis() - 60 * 1000)
        {
            startRoomInTask(mLocation);
            return;
        }

        int p = getPackageManager().checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, getPackageName());
        if(p == PackageManager.PERMISSION_DENIED) {

            AlertDialog.Builder confirmAlert = new AlertDialog.Builder(RoomEnterActivity.this);
            confirmAlert.setTitle(getString(R.string.text_confirm));
            confirmAlert.setMessage(getString(R.string.error_gps_needed));

            confirmAlert.setPositiveButton(getString(R.string.text_setting), new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which) {
                    enableLocationSettings();
                }
            });

            confirmAlert.setNegativeButton(getString(R.string.action_cancel), new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which) {
                }});
            return;
        }

        // if the client is not connected, get the connection
        if(!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_enter);

        // Set up room in components.
        mRoomKeyView = (EditText) findViewById(R.id.room_key_text);

        KeyListener keyListener = DigitsKeyListener.getInstance("1234567890");
        mRoomKeyView.setKeyListener(keyListener);
        mRoomKeyView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.room_in || id == EditorInfo.IME_NULL) {
                    attemptRoomIn();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.room_key_button);
        if(mEmailSignInButton != null)
            mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRoomIn();
            }
        });

        mProgressView = findViewById(R.id.roomin_progress);
        mKeyAreaView = findViewById(R.id. key_input_area);
        mRootLayout = (LinearLayout) findViewById(R.id.root_layout);

        React mApp = (React) this.getApplication();
        mApiToken = mApp.getApiToken();

       // mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }


        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mRoomKeyView.requestFocus();
        mInputMethodManager.showSoftInput(mRoomKeyView, InputMethodManager.SHOW_IMPLICIT);
    }
    @Override
    protected void onStart()
    {
        if(!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        super.onStart();
    }
    @Override
    protected void onStop()
    {
        if(mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
        super.onStop();
    }
    @Override
    public void onConnected(Bundle connectionHint) {
        int p = getPackageManager().checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, getPackageName());
        if(p == PackageManager.PERMISSION_DENIED) {

            AlertDialog.Builder confirmAlert = new AlertDialog.Builder(RoomEnterActivity.this);
            confirmAlert.setTitle(getString(R.string.text_confirm));
            confirmAlert.setMessage(getString(R.string.error_gps_needed));

            confirmAlert.setPositiveButton(getString(R.string.text_setting), new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which) {
                    enableLocationSettings();
                }
            });

            confirmAlert.setNegativeButton(getString(R.string.action_cancel), new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which) {
                }});
            return;
        }

        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (lastLocation != null) {
            mLocation = lastLocation;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // When GoogleApiClient is interrupted
        // i can be GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST
        //     or GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // When GoogleApiClient failed
        // to get detailed errors
        // result.getErrorCode();
    }

    @Override
    public void onLocationChanged(Location location)
    {
        mLocation = location;
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        startRoomInTask(mLocation);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        mInputMethodManager.hideSoftInputFromWindow(mRootLayout.getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
        mRootLayout.requestFocus();
        return true;
    }

    /**
     * Attempts to register the account specified by the roomin form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual roomin attempt is made.
     */
    private void attemptRoomIn() {
        if (mRoomConfirmTask != null)
        {
            return;
        }

        // Reset errors.
        mRoomKeyView.setError(null);

        // Store values at the time of the roomin attempt.
        mRoomKey = mRoomKeyView.getText().toString();

        boolean cancel = false;

        // Check for a valid room key, if the user entered one.
        if (TextUtils.isEmpty(mRoomKey))
        {
            mRoomKeyView.setError(getString(R.string.error_field_required));
            cancel = true;
        }
        else if(!isKeyValid(mRoomKey))
        {
            mRoomKeyView.setError(getString(R.string.error_invalid_room_key));
            cancel = true;
        }

        if (cancel)
        {
            // There was an error; don't attempt roomin and focus the first
            // form field with an error.
            mRoomKeyView.requestFocus();
        }
        else
        {
            // Show a progress spinner, and kick off a background task to
            // perform the user roomin attempt.
            showProgress(true);
            final String url = getString(R.string.domain) + "/student/rooms/"+mRoomKey+"?api_token="+mApiToken;
            mRoomConfirmTask = new RoomConfirmTask(url);
            mRoomConfirmTask.execute((Void) null);
        }
    }

    private boolean isKeyValid(String key)
    {
        return (key.length() == 6);
    }

    /**
     * Shows the progress UI and hides the roomin form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show)
    {
        if(show)
        {
            mInputMethodManager.hideSoftInputFromWindow(mRootLayout.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
            mRootLayout.requestFocus();
        }

        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2)
        {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mKeyAreaView.setVisibility(show ? View.GONE : View.VISIBLE);
            mKeyAreaView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mKeyAreaView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        }
        else
        {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mKeyAreaView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous room check task used to authenticate
     * the user.
     */
    public class RoomConfirmTask extends AsyncTask<Void, Void, JSONObject>
    {
        private final String mUrl;
        private Integer mResponseCode = 0;
        private  boolean isTimeOut = false;

        RoomConfirmTask(String url)
        {
            mUrl = url;
        }

        @Override
        protected JSONObject doInBackground(Void... params)
        {
            Log.d(TAG, "RoomConfirmTask Start");
            HttpURLConnection con = null;
            URL url;

            JSONObject jsonOutput = null;
            BufferedReader bufStr;

            try {
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
                mResponseCode = con.getResponseCode();

                mResponseCode = con.getResponseCode();
                if (HttpURLConnection.HTTP_OK == mResponseCode)
                    bufStr = new BufferedReader(new InputStreamReader(con.getInputStream()));
                else
                    bufStr = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                String body = bufStr.readLine();
                jsonOutput = new JSONObject(body);
            }
            catch (java.net.UnknownHostException|java.net.SocketTimeoutException e) {
                isTimeOut = true;
                e.printStackTrace();
            }
            catch (JSONException|IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                if( con != null )
                    con.disconnect();
            }
            return jsonOutput;
        }

        @Override
        protected void onPostExecute(final JSONObject result)
        {
            Log.d(TAG, "RoomConfirmTask Finish");

            mRoomConfirmTask = null;
            showProgress(false);

            if (HttpURLConnection.HTTP_OK == mResponseCode)
            {
                try
                {
                    mTeacherName = result.getString("teacher");
                    mLectureName = result.getString("lecture");
                    mTimeSlot = result.getString("timeslot");
                }catch (JSONException e)
                {
                    e.printStackTrace();
                }

                AlertDialog.Builder confirmAlert = new AlertDialog.Builder(RoomEnterActivity.this);
                confirmAlert.setTitle(getString(R.string.text_confirm_room));
                confirmAlert.setMessage(mTeacherName+"\n"+mLectureName+"\n"+mTimeSlot);

                confirmAlert.setPositiveButton(getString(R.string.action_room_in), new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                    showProgress(true);
                    registerLocationUpdates();
                    }
                });

                confirmAlert.setNegativeButton(getString(R.string.action_cancel), new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                    }});

                // show dialog
                confirmAlert.show();
                return;
            }

            String errMsg;
            if(isTimeOut)
            {
                errMsg = getString(R.string.error_timeout);
            }
            else if(result == null)
            {
                errMsg = getString(R.string.error_unknown);
            }
            else
            {
                try
                {
                    String type = result.getString("type");
                    String message = result.getString("message");
                    String[] info = type.split("\\.");
                    if (info[0].equals("room"))
                    {
                        mRoomKeyView.setError(message);
                        mRoomKeyView.requestFocus();
                        return;
                    }
                    errMsg = message;
                }
                catch (JSONException e)
                {
                    errMsg = getString(R.string.error_unknown);
                    e.printStackTrace();
                }
            }

            final LinearLayout layout = (LinearLayout) findViewById(R.id.root_layout);
            if(layout != null)
                Snackbar.make(layout, errMsg, Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.text_resend), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        attemptRoomIn();
                    }
                })
                .show();
        }

        @Override
        protected void onCancelled()
        {
            mRoomConfirmTask = null;
            showProgress(false);
        }
    }

    private void startRoomInTask(Location location)
    {
        final String roomInUrl = getString(R.string.domain) + "/student/rooms/" + mRoomKey + "?api_token=" + mApiToken;
        Log.d(TAG,"Room In Location: "+String.valueOf(location.getLatitude())+" "+String.valueOf(location.getLongitude()));
        mRoomInTask = new RoomInTask(location.getLatitude(), location.getLongitude(), roomInUrl);
        mRoomInTask.execute((Void) null);
    }

    /**
     * Represents an asynchronous room in task used to authenticate
     * the user.
     */
    private class RoomInTask extends AsyncTask<Void, Void, JSONObject>
    {
        private final String mUrl;
        private final double mLat;
        private final double mLong;
        private Integer mResponseCode = 0;
        private  boolean isTimeOut = false;

        RoomInTask(double geoLat, double geoLong, String url)
        {
            mLat = geoLat;
            mLong = geoLong;
            mUrl = url;
        }

        @Override
        protected JSONObject doInBackground(Void... params)
        {
            Log.d(TAG, "RoomInTask Start");
            HttpURLConnection con = null;
            URL url;

            JSONObject jsonOutput = null;
            BufferedReader bufStr;

            try
            {
                JSONObject jsonInput = new JSONObject();
                Resources res = getResources();
                jsonInput.put("action", res.getInteger(R.integer.action_basic));
                jsonInput.put("type", res.getInteger(R.integer.type_room_in));
                jsonInput.put("geo_lat", mLat);
                jsonInput.put("geo_long", mLong);

                url = new URL(mUrl);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setInstanceFollowRedirects(false);
                con.setRequestProperty("Accept-Language", "ja");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Content-Type", "application/json");
                con.setUseCaches(false);
                con.setAllowUserInteraction(false);
                con.setConnectTimeout(getResources().getInteger(R.integer.delay_http_connect));
                con.setReadTimeout(getResources().getInteger(R.integer.delay_http_read));
                con.setDoInput(true);
                con.setDoOutput(true);

                OutputStream os = con.getOutputStream();
                os.write(jsonInput.toString().getBytes());
                os.flush();
                os.close();

                mResponseCode = con.getResponseCode();
                if (HttpURLConnection.HTTP_OK == mResponseCode)
                    bufStr = new BufferedReader(new InputStreamReader(con.getInputStream()));
                else
                    bufStr = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                String body = bufStr.readLine();
                jsonOutput = new JSONObject(body);
            } catch (java.net.UnknownHostException|java.net.SocketTimeoutException e) {
                isTimeOut = true;
                e.printStackTrace();
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            } finally {
                if (con != null)
                    con.disconnect();
            }
            return jsonOutput;
        }

        @Override
        protected void onPostExecute(final JSONObject result)
        {
            Log.d(TAG, "RoomInTask Finish");
            mRoomInTask = null;

            String errMsg = null;

            if (HttpURLConnection.HTTP_OK == mResponseCode)
            {
                Intent intent = new Intent(RoomEnterActivity.this, ReactionActivity.class);
                intent.putExtra("ROOM_KEY", mRoomKey);
                intent.putExtra("TEACHER_NAME", mTeacherName);
                intent.putExtra("LECTURE_NAME", mLectureName);
                intent.putExtra("TIME_SLOT", mTimeSlot);
                startActivity(intent);
                return;
            }

            showProgress(false);

            if(isTimeOut)
            {
                errMsg = getString(R.string.error_timeout);
            }
            else if(result == null)
            {
                errMsg = getString(R.string.error_unknown);
            }
            else {
                try {
                    String type = result.getString("type");
                    String message = result.getString("message");
                    String[] info = type.split("\\.");
                    if (info[0].equals("room")) {
                        if (info[1].equals("already_room_in")) {
                            Intent intent = new Intent(RoomEnterActivity.this, ReactionActivity.class);
                            intent.putExtra("ROOM_KEY", mRoomKey);
                            intent.putExtra("TEACHER_NAME", mTeacherName);
                            intent.putExtra("LECTURE_NAME", mLectureName);
                            intent.putExtra("TIME_SLOT", mTimeSlot);
                            startActivity(intent);
                            return;
                        }
                    }
                    mRoomKeyView.setError(message);
                    mRoomKeyView.requestFocus();
                } catch (JSONException | NullPointerException e) {
                    errMsg = getString(R.string.error_unknown);
                    e.printStackTrace();
                }
            }

            if(!TextUtils.isEmpty(errMsg))
            {
                final LinearLayout layout = (LinearLayout) findViewById(R.id.root_layout);
                if(layout != null)
                    Snackbar.make(layout, errMsg, Snackbar.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled()
        {
            mRoomInTask = null;
            showProgress(false);
        }
    }

    private void enableLocationSettings()
    {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(settingsIntent);
    }
}
