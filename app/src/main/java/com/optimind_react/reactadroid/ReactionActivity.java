package com.optimind_react.reactadroid;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class ReactionActivity extends AppCompatActivity implements View.OnClickListener {
    private ReactionActivity selfClass = this;
    private String mApiToken;
    private Resources mRes;
    private ReactTask mReactTask = null;
    private StatusTask mStatusTask = null;
    // UI Threadへのpost用ハンドラ
    private Handler mHandler = new Handler();
    // show room key
    private String mRoomKey;
    private TextView mTimePast, mClickNum, mNumConfused, mNumInteresting, mNumBoring;

    private String mStatusUrl, mReactUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reaction);

        Intent intent = getIntent();
        mRoomKey = intent.getStringExtra("ROOM_KEY");//設定したkeyで取り出す

        ((TextView)findViewById(R.id.TeacherName)).setText(intent.getStringExtra("TEACHER_NAME"));
        ((TextView)findViewById(R.id.LectureName)).setText(intent.getStringExtra("LECTURE_NAME"));
        ((TextView)findViewById(R.id.TimeSlot)).setText(intent.getStringExtra("TIME_SLOT"));

        mRes = getResources();

        mClickNum = (TextView)findViewById(R.id.ClickNum);
        mClickNum.setText("0");
        mTimePast = (TextView)findViewById(R.id.TimePast);
        mTimePast.setText("0");
        mNumConfused = (TextView)findViewById(R.id.NumConfused);
        mNumConfused.setText("0");
        mNumInteresting = (TextView)findViewById(R.id.NumInteresting);
        mNumInteresting.setText("0");
        mNumBoring = (TextView)findViewById(R.id.NumBoring);
        mNumBoring.setText("0");

        ((Button)findViewById(R.id.ConfusedButton)).setOnClickListener(this);
        ((Button)findViewById(R.id.InterestingButton)).setOnClickListener(this);
        ((Button)findViewById(R.id.BoringButton)).setOnClickListener(this);
        ((ImageButton)findViewById(R.id.MessageButton)).setOnClickListener(this);

        React mApp = (React) this.getApplication();
        mApiToken = mApp.getApiToken();

        mReactUrl = getString(R.string.domain) + "/student/rooms/"+mRoomKey+"?api_token="+mApiToken;
        mStatusUrl = getString(R.string.domain) + "/student/rooms/"+mRoomKey+"/status?api_token="+mApiToken;

        mStatusTask = new StatusTask( mStatusUrl, "GET");
        mStatusTask.execute((Void) null);
    }

    public void onClick(View v) {
        if(v.getId() == R.id.MessageButton)
        {
            Intent intent = new Intent(selfClass, MessageActivity.class);
            intent.putExtra("ROOM_KEY", mRoomKey);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
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
        // calculate the click number
        Integer clickNum = Integer.parseInt(mClickNum.getText().toString())+1;
        mClickNum.setText(String.valueOf(clickNum));

        // make button invisible
        v.setClickable(false);
        //v.setVisibility(View.INVISIBLE);
        v.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);

        //タイマースケジュール設定＆開始
        //タイマーインスタンス生成
        Timer reactTimer = new Timer();
        //タスククラスインスタンス生成
        reactTimer.schedule(new ReactTimerTask(v), mRes.getInteger(R.integer.timer_react_button_mask));

        mReactTask = new ReactTask(mRes.getInteger(R.integer.action_reaction_anonymous), type, mReactUrl, "POST");
        mReactTask.execute((Void) null);
    }
    @Override
    public void onBackPressed() {
        AlertDialog.Builder confirmAlert = new AlertDialog.Builder(this);
        //ダイアログタイトルをセット
        confirmAlert.setTitle(getString(R.string.title_quit_room));
        //ダイアログメッセージをセット
        confirmAlert.setMessage(getString(R.string.text_quit_room));

        // アラートダイアログのボタンがクリックされた時に呼び出されるコールバックリスナーを登録します
        confirmAlert.setPositiveButton(getString(R.string.action_cancel), new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
            }});

        // アラートダイアログのボタンがクリックされた時に呼び出されるコールバックリスナーを登録します
        confirmAlert.setNegativeButton(getString(R.string.action_room_out), new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
                mReactTask = new ReactTask(mRes.getInteger(R.integer.action_basic), mRes.getInteger(R.integer.type_room_out), mReactUrl, "POST");
                mReactTask.execute((Void) null);
                finish();
            }
        });
        //ダイアログ表示
        confirmAlert.show();
    }

    /**
     * Represents an asynchronous room check task used to authenticate
     * the user.
     */
    private class ReactTask extends AsyncTask<Void, Void, Integer> {

        private final int mAction;
        private final int mType;
        private final String mUrl;
        private final String mMethod;
        private String responseBody;

        ReactTask(Integer action, Integer type, String url, String method)
        {
            mAction = action;
            mType = type;
            mUrl = url;
            mMethod = method;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // httpのコネクションを管理するクラス
            HttpURLConnection con = null;
            URL url = null;
            int status = 0;
            JSONObject jsonObj = new JSONObject();
            // InputStreamからbyteデータを取得するための変数
            BufferedReader bufStr = null;

            try {
                jsonObj.put("action", mAction);
                jsonObj.put("type", mType);
                // URLの作成
                url = new URL(mUrl);
                // 接続用HttpURLConnectionオブジェクト作成
                con = (HttpURLConnection)url.openConnection();
                // リクエストメソッドの設定
                con.setRequestMethod(mMethod);
                // リダイレクトを自動で許可しない設定
                con.setInstanceFollowRedirects(false);
                con.setRequestProperty("Accept-Language", "jp");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Content-Type", "application/json");
                con.setUseCaches(false);
                con.setAllowUserInteraction(false);
                con.setConnectTimeout(mRes.getInteger(R.integer.delay_http_connect));
                con.setReadTimeout(mRes.getInteger(R.integer.delay_http_read));

                con.setDoOutput(true);
                OutputStream os = con.getOutputStream();
                os.write(jsonObj.toString().getBytes());
                os.flush();
                os.close();

                status = con.getResponseCode();

                bufStr = new BufferedReader(new InputStreamReader(con.getInputStream()));
                responseBody = bufStr.readLine();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if( con != null ){
                    con.disconnect();
                }
            }

            return status;
        }

        @Override
        protected void onPostExecute(final Integer status) {
            mReactTask = null;}

        @Override
        protected void onCancelled() {
            mReactTask = null;
        }
    }


    /**
     * Represents an asynchronous room check task used to authenticate
     * the user.
     */
    private class StatusTask extends AsyncTask<Void, Void, JSONObject> {
        private final String mUrl;
        private final String mMethod;
        private String responseBody;

        StatusTask(String url, String method)
        {
            mUrl = url;
            mMethod = method;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            // httpのコネクションを管理するクラス
            HttpURLConnection con = null;
            URL url = null;
            int status = 0;
            JSONObject jsonObj = null;
            // InputStreamからbyteデータを取得するための変数
            BufferedReader bufStr = null;

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
                con.setConnectTimeout(mRes.getInteger(R.integer.delay_http_connect));
                con.setReadTimeout(mRes.getInteger(R.integer.delay_http_read));
                con.connect();
                status = con.getResponseCode();


                if (HttpURLConnection.HTTP_OK == status)
                {
                    bufStr = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    responseBody = bufStr.readLine();
                    jsonObj = new JSONObject(responseBody);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if( con != null ){
                    con.disconnect();
                }
            }

            return jsonObj;
        }

        @Override
        protected void onPostExecute(final JSONObject jsonObj) {
            mStatusTask = null;
            if(jsonObj == null)
            {
                AlertDialog.Builder alertQuitRoom = new AlertDialog.Builder(selfClass);
                //ダイアログタイトルをセット
                alertQuitRoom.setTitle(getString(R.string.title_quit_room));
                //ダイアログメッセージをセット
                alertQuitRoom.setMessage(getString(R.string.info_quit_room));
                alertQuitRoom.setCancelable(false);
                // ボタンがクリックされた時に呼び出されるコールバックリスナーを登録します
                alertQuitRoom.setPositiveButton("OK",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                        //退室処理
                        mReactTask = new ReactTask(mRes.getInteger(R.integer.action_basic), mRes.getInteger(R.integer.type_room_out), mReactUrl, "POST");
                        mReactTask.execute((Void) null);
                        finish();
                    }});
                //ダイアログ表示
                alertQuitRoom.show();
            }
            else
            {
                try {
                    mNumConfused.setText(jsonObj.getString("num_confused"));
                    mNumInteresting.setText(jsonObj.getString("num_interesting"));
                    mNumBoring.setText(jsonObj.getString("num_boring"));
                    mTimePast.setText(jsonObj.getString("timediff_room_in"));
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }

                //タイマースケジュール設定＆開始
                //タイマーインスタンス生成
                Timer reactTimer = new Timer();
                //タスククラスインスタンス生成
                reactTimer.schedule(new StatusTimerTask(), mRes.getInteger(R.integer.timer_require_status));
            }
        }

        @Override
        protected void onCancelled() {
            mStatusTask = null;
        }
    }

    private class ReactTimerTask extends TimerTask {

        private final View mView;


        ReactTimerTask ( View view )
        {
            mView = view;
        }

        public void run() {
            //ここに定周期で実行したい処理を記述します
            mHandler.post( new Runnable() {
                public void run() {
                    //reactButton.setVisibility(View.VISIBLE);
                    mView.setClickable(true);
                    mView.getBackground().setColorFilter(null);
                }
            });
        }
    }

    private class StatusTimerTask extends TimerTask {
        public void run() {
            if(mStatusTask != null)
                return;
            mStatusTask = new StatusTask( mStatusUrl, "GET");
            mStatusTask.execute((Void) null);
        }
    }
}