package com.optimind_react.reactadroid;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
    private ReactTask mReactTask = null;
    // 押した回数のカウンター
    private int clickNum = 0;
    // UI Threadへのpost用ハンドラ
    private Handler mHandler = new Handler();
    // show room key
    private String teacherName;
    private String lectureName;
    private String timeSlot;
    private String roomKey;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reaction);

        Intent intent = getIntent();
        roomKey = intent.getStringExtra("ROOM_KEY");//設定したkeyで取り出す
        teacherName = intent.getStringExtra("TEACHER_NAME");//設定したkeyで取り出す
        lectureName = intent.getStringExtra("LECTURE_NAME");//設定したkeyで取り出す
        timeSlot = intent.getStringExtra("TIME_SLOT");//設定したkeyで取り出す
        final Button reactButton = (Button)findViewById(R.id.reactButton);
        reactButton.setOnClickListener(this);

        React mApp = (React) this.getApplication();
        mApiToken = mApp.getApiToken();
    }

    public void onClick(View v) {
        Timer reactTimer;					//タイマー用
        ReactTimerTask reactTimerTask;		//タイマタスククラス
        switch (v.getId()) {
            case R.id.reactButton:
                // make button invisible
                v.setClickable(false);
                v.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
                //v.setVisibility(View.INVISIBLE);

                // update the react button with current number of pressed people
                TextView currentReactText =
                        (TextView)findViewById(R.id.currentReactText);
                currentReactText.setText("現在反応人数：3");

                // calculate the click number
                clickNum++;
                TextView clickNumText =
                        (TextView)findViewById(R.id.clickNumText);
                clickNumText.setText("押した回数："+clickNum);

                //タイマースケジュール設定＆開始
                //タイマーインスタンス生成
                reactTimer = new Timer();
                //タスククラスインスタンス生成
                reactTimerTask = new ReactTimerTask();
                reactTimer.schedule(reactTimerTask, 5000);
                break;

            default:
                break;
        }
    }
    /**
     * タイマータスク派生クラス
     * run()に定周期で処理したい内容を記述
     *
     */
    public class ReactTimerTask extends TimerTask {
        @Override
        public void run() {
            //ここに定周期で実行したい処理を記述します
            mHandler.post( new Runnable() {
                public void run() {
                    final Button reactButton = (Button)findViewById(R.id.reactButton);
                    // make button visible
                    //reactButton.setVisibility(View.VISIBLE);
                    reactButton.setClickable(true);
                    reactButton.getBackground().setColorFilter(null);

                    // update the react button with current number of pressed people
                    TextView currentReactText =
                            (TextView)findViewById(R.id.currentReactText);
                    currentReactText.setText(R.string.push_button_string);
                }
            });
        }
    }
    @Override
    public void onBackPressed() {
        // your code.
        final String roomInUrl = getString(R.string.domain) + "/student/rooms/"+roomKey+"?api_token="+mApiToken;
        mReactTask = new ReactTask(1, 2,  roomInUrl, "POST");
        mReactTask.execute((Void) null);
        super.onBackPressed();
    }

    /**
     * Represents an asynchronous room check task used to authenticate
     * the user.
     */
    public class ReactTask extends AsyncTask<Void, Void, Integer> {

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
                con.setConnectTimeout(3000);
                con.setReadTimeout(3000);

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
}