package com.optimind_react.reactadroid;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ReactionActivity extends AppCompatActivity implements View.OnClickListener
{
    // tag for debug
    private final static String TAG = ReactionActivity.class.getSimpleName();

    // task
    private Resources mRes;
    private ReactTask mReactTask = null;
    private StatusTask mStatusTask = null;
    private String mStatusUrl, mReactUrl;
    private String mRoomKey;

    // handler
    private final Handler mReactTimerHandler = new Handler();
    private final Handler mStatusTimerHandler = new Handler();

    // UI components
    private TextView mTimePast, mClickNum, mNumConfused, mNumInteresting, mNumBoring;

    // flag
    private boolean isStatusTimerRun = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reaction);

        Intent intent = getIntent();
        mRoomKey = intent.getStringExtra("ROOM_KEY");//設定したkeyで取り出す

        TextView teacherName = (TextView)findViewById(R.id.TeacherName);
        if(teacherName != null)
            teacherName.setText(intent.getStringExtra("TEACHER_NAME"));
        TextView lectureName = (TextView)findViewById(R.id.LectureName);
        if(lectureName != null)
            lectureName.setText(intent.getStringExtra("LECTURE_NAME"));
        TextView timeSlot = (TextView)findViewById(R.id.TimeSlot);
        if(timeSlot != null)
            timeSlot.setText(intent.getStringExtra("TIME_SLOT"));

        mRes = getResources();

        mClickNum = (TextView)findViewById(R.id.ClickNum);
        if(mClickNum != null)
            mClickNum.setText("0");
        mTimePast = (TextView)findViewById(R.id.TimePast);
        if(mTimePast != null)
            mTimePast.setText("0");
        mNumConfused = (TextView)findViewById(R.id.NumConfused);
        if(mNumConfused != null)
            mNumConfused.setText("0");
        mNumInteresting = (TextView)findViewById(R.id.NumInteresting);
        if(mNumInteresting != null)
            mNumInteresting.setText("0");
        mNumBoring = (TextView)findViewById(R.id.NumBoring);
        if(mNumBoring != null)
            mNumBoring.setText("0");

        Button confusedButton = (Button)findViewById(R.id.ConfusedButton);
        if(confusedButton != null)
            confusedButton.setOnClickListener(this);
        Button interestingButton = (Button)findViewById(R.id.InterestingButton);
        if(interestingButton != null)
            interestingButton.setOnClickListener(this);
        Button boringButton = (Button)findViewById(R.id.BoringButton);
        if(boringButton != null)
            boringButton.setOnClickListener(this);
        Button messageButton = (Button)findViewById(R.id.MessageButton);
        if(messageButton != null)
            messageButton.setOnClickListener(this);

        React mApp = (React) this.getApplication();
        String apiToken = mApp.getApiToken();

        mReactUrl = getString(R.string.domain) + "/student/rooms/"+mRoomKey+"?api_token="+apiToken;
        mStatusUrl = getString(R.string.domain) + "/student/rooms/"+mRoomKey+"/status?api_token="+apiToken;

        mStatusTask = new StatusTask(mStatusUrl);
        mStatusTask.execute((Void) null);

        IntentFilter regFilter = new IntentFilter();
        // get device shutdown or reboot event
        regFilter.addAction(Intent.ACTION_REBOOT);
        regFilter.addAction(Intent.ACTION_SHUTDOWN);
        registerReceiver(receiver, regFilter );
    }

    public void onClick(View v)
    {
        if(v.getId() == R.id.MessageButton)
        {
            Intent intent = new Intent(ReactionActivity.this, MessageActivity.class);
            intent.putExtra("ROOM_KEY", mRoomKey);
            startActivity(intent);
            return;
        }
        Integer type = mRes.getInteger(R.integer.type_confused);
        switch (v.getId()) {
            case R.id.ConfusedButton:
                type = mRes.getInteger(R.integer.type_confused);
                break;
            case R.id.InterestingButton:
                type = mRes.getInteger(R.integer.type_interesting);
                break;
            case R.id.BoringButton:
                type = mRes.getInteger(R.integer.type_boring);
                break;
            default:
                break;
        }

        // make button invisible
        v.setClickable(false);
        v.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);

        mReactTimerHandler.postDelayed(new ReactTimerTask(v), mRes.getInteger(R.integer.timer_react_button_mask));

        mReactTask = new ReactTask(mRes.getInteger(R.integer.action_reaction_anonymous), type, mReactUrl);
        mReactTask.execute((Void) null);
    }

    @Override
    public void onBackPressed()
    {
        // create and show dialog
        AlertDialog.Builder confirmAlert = new AlertDialog.Builder(this);
        confirmAlert.setTitle(getString(R.string.title_quit_room));
        confirmAlert.setMessage(getString(R.string.text_quit_room));

        // press OK
        confirmAlert.setPositiveButton(getString(R.string.action_room_out), new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
                mReactTask = new ReactTask(mRes.getInteger(R.integer.action_basic), mRes.getInteger(R.integer.type_room_out), mReactUrl);
                mReactTask.execute((Void) null);
                finish();
            }
        });

        // press cancel
        confirmAlert.setNegativeButton(getString(R.string.action_cancel), new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
            }});

        // show dialog
        confirmAlert.show();
    }

    // for foreground <-> background event
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(final Context context, final Intent intent) {
            //check if the broadcast is our desired one
            if (intent.getAction().equals(Intent.ACTION_REBOOT))
            {
                Log.d(TAG, "ACTION_REBOOT");
                mReactTask = new ReactTask(mRes.getInteger(R.integer.action_basic), mRes.getInteger(R.integer.type_room_out), mReactUrl);
                mReactTask.execute((Void) null);
            }
            else if (intent.getAction().equals(Intent.ACTION_SHUTDOWN))
            {
                Log.d(TAG, "ACTION_SHUTDOWN");
                mReactTask = new ReactTask(mRes.getInteger(R.integer.action_basic), mRes.getInteger(R.integer.type_room_out), mReactUrl);
                mReactTask.execute((Void) null);
            }
        }
    };

    @Override
    public void onStop()
    {
        super.onStop();

        // happened by the app itself
        React mApp = (React) this.getApplication();
        if(mApp.isForeground())
            return;

        // happened by turn off the power
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            if(!pm.isInteractive())
                return;
        }

        Log.d(TAG, "onStop");
        mReactTask = new ReactTask(mRes.getInteger(R.integer.action_basic), mRes.getInteger(R.integer.type_fore_out), mReactUrl);
        mReactTask.execute((Void) null);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        if(!isStatusTimerRun && mStatusTask == null)
        {
            mStatusTask = new StatusTask(mStatusUrl);
            mStatusTask.execute((Void) null);
        }

        // happened by the app itself
        React mApp = (React) this.getApplication();
        if(mApp.getAppStatus() != React.AppStatus.RETURNED_TO_FOREGROUND)
            return;

        // start by screen on
        if(!mApp.getScreenStatus())
            return;

        Log.d(TAG, "onStart");
        mReactTask = new ReactTask(mRes.getInteger(R.integer.action_basic), mRes.getInteger(R.integer.type_fore_in), mReactUrl);
        mReactTask.execute((Void) null);
    }

    class ReactTimerTask implements Runnable
    {
        private final View mView;
        ReactTimerTask ( View view )
        {
            mView = view;
        }
        public void run()
        {
            mView.setClickable(true);
            mView.getBackground().setColorFilter(null);
        }
    }

    // timer run function
    private final Runnable statusTimeTask = new Runnable()
    {
        @SuppressLint("InlinedApi")
        @Override
        public void run()
        {
            Log.d(TAG, "onStatusTimerRun");
            // flag
            isStatusTimerRun = false;
            if(mStatusTask == null) {
                mStatusTask = new StatusTask(mStatusUrl);
                mStatusTask.execute((Void) null);
            }
        }
    };

    /**
     * Represents an asynchronous reaction send task
     */
    private class ReactTask extends AsyncTask<Void, Void, JSONObject>
    {
        private final String mUrl;
        private final int mAction;
        private final int mType;
        private Integer mResponseCode = 0;
        private boolean isTimeOut = false;

        ReactTask(Integer action, Integer type, String url)
        {
            mAction = action;
            mType = type;
            mUrl = url;
        }

        @Override
        protected JSONObject doInBackground(Void... params)
        {
            Log.d(TAG, "ReactTask Start");
            HttpURLConnection con = null;
            URL url;
            JSONObject jsonOutput = null;
            BufferedReader bufStr;

            try
            {
                JSONObject jsonInput = new JSONObject();
                jsonInput.put("action", mAction);
                jsonInput.put("type", mType);

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
            Log.d(TAG, "ReactTask finish");
            mReactTask = null;

            if(HttpURLConnection.HTTP_OK == mResponseCode)
            {
                if (mAction == getResources().getInteger(R.integer.action_reaction_anonymous) ||
                        mAction == getResources().getInteger(R.integer.action_reaction_real_name))
                {
                    // calculate the click number
                    Integer clickNum = Integer.parseInt(mClickNum.getText().toString()) + 1;
                    mClickNum.setText(String.valueOf(clickNum));

                    if(mStatusTask == null)
                    {
                        mStatusTimerHandler.removeCallbacks(statusTimeTask);
                        mStatusTask = new StatusTask(mStatusUrl);
                        mStatusTask.execute((Void) null);
                    }
                }
                return;
            }

            String errMsg = null;
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
                    if(info[0].equals("room")) {
                        switch (info[1]) {
                            case "already_room_in":
                            case "already_room_out":
                            case "already_fore_in":
                            case "already_fore_out":
                                break;
                            default:
                                errMsg = message;
                                break;
                        }
                    }
                    else
                    {
                        errMsg = message;
                    }
                }
                catch (JSONException e)
                {
                    errMsg = getString(R.string.error_unknown);
                    e.printStackTrace();
                }
            }

            if(!TextUtils.isEmpty(errMsg)) {
                final LinearLayout layout = (LinearLayout) findViewById(R.id.root_layout);
                if(layout != null)
                    Snackbar.make(layout, errMsg, Snackbar.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {mReactTask = null;}
    }


    /**
     * Represents an asynchronous status check task
     */
    private class StatusTask extends AsyncTask<Void, Void, JSONObject>
    {
        private final String mUrl;
        private Integer mResponseCode = 0;
        private boolean isTimeOut = false;

        StatusTask(String url)
        {
            mUrl = url;
        }

        @Override
        protected JSONObject doInBackground(Void... params)
        {
            Log.d(TAG, "StatusTask Start");
            HttpURLConnection con = null;
            URL url;

            JSONObject jsonOutput = null;
            BufferedReader bufStr;

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
            Log.d(TAG, "StatusTask Finish");
            mStatusTask = null;
            boolean isStartTimer = isTimeOut;

            if (HttpURLConnection.HTTP_OK == mResponseCode)
            {
                try
                {
                    mNumConfused.setText(result.getString("num_confused"));
                    mNumInteresting.setText(result.getString("num_interesting"));
                    mNumBoring.setText(result.getString("num_boring"));
                    mTimePast.setText(result.getString("timediff_room_in"));
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }

                isStartTimer = true;
            }
            else
            {
                String message = getString(R.string.error_unknown);
                if (result != null)
                {
                    try
                    {
                        String type = result.getString("type");
                        String[] info = type.split("\\.");
                        if (info[0].equals("room") && info[1].equals("closed"))
                            message = result.getString("message");
                    } catch (JSONException e)
                    {
                        isStartTimer = true;
                        e.printStackTrace();
                    }
                }

                if(!isStartTimer) {
                    AlertDialog.Builder alertQuitRoom = new AlertDialog.Builder(ReactionActivity.this);
                    alertQuitRoom.setTitle(getString(R.string.title_quit_room));
                    alertQuitRoom.setMessage(message + getString(R.string.info_quit_room));
                    alertQuitRoom.setCancelable(false);

                    alertQuitRoom.setPositiveButton(getString(R.string.action_ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // quit room
                            mReactTask = new ReactTask(mRes.getInteger(R.integer.action_basic), mRes.getInteger(R.integer.type_room_out), mReactUrl);
                            mReactTask.execute((Void) null);
                            finish();
                        }
                    });
                    //ダイアログ表示
                    alertQuitRoom.show();
                }
            }

            if(isStartTimer)
            {
                // start a new timer to get status
                mStatusTimerHandler.postDelayed(statusTimeTask, mRes.getInteger(R.integer.timer_require_status));
                Log.d(TAG, "StatusTimer Start");
                // flag
                isStatusTimerRun = true;
            }
        }

        @Override
        protected void onCancelled()
        {
            mStatusTask = null;
        }
    }
}