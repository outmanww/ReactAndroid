package com.optimind_react.reactadroid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class RoomEnterActivity extends AppCompatActivity{

    private RoomConfirmTask mRoomConfirmTask = null;
    private RoomInTask mRoomInTask = null;

    // UI references.
    private EditText mRoomKeyView;
    private View mProgressView;
    private View mKeyAreaView;
    private RoomEnterActivity selfClass = this;

    // http result
    private String teacherName;
    private String lectureName;
    private String timeSlot;
    private String roomKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_enter);

        // Set up room in components.
        mRoomKeyView = (EditText) findViewById(R.id.roomKeyText);

//        mRoomKeyView.setInputType(InputType.TYPE_CLASS_NUMBER);
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

        Button mEmailSignInButton = (Button) findViewById(R.id.roomKeyButton);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRoomIn();
            }
        });

        mProgressView = findViewById(R.id.roomin_progress);
        mKeyAreaView = findViewById(R.id. key_input_area);
    }

    /**
     * Attempts to register the account specified by the roomin form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual roomin attempt is made.
     */
    private void attemptRoomIn() {
        if (mRoomConfirmTask != null) {
            return;
        }

        // Reset errors.
        mRoomKeyView.setError(null);

        // Store values at the time of the roomin attempt.
        roomKey = mRoomKeyView.getText().toString();

        boolean cancel = false;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(roomKey) || !isKeyValid(roomKey)) {
            mRoomKeyView.setError(getString(R.string.error_invalid_room_key));
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt roomin and focus the first
            // form field with an error.
            mRoomKeyView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user roomin attempt.
            showProgress(true);
            React mApp = (React) this.getApplication();
            String apiToken = mApp.getApiToken();
            final String url = getString(R.string.domain) + "/student/rooms/"+roomKey+"?api_token="+apiToken;
            mRoomConfirmTask = new RoomConfirmTask(apiToken,  url, "GET");
            mRoomConfirmTask.execute((Void) null);
        }
    }
        private boolean isKeyValid(String key) {
            return (key.length() == 6);
        }

    /**
     * Shows the progress UI and hides the roomin form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
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
        } else {
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
    public class RoomConfirmTask extends AsyncTask<Void, Void, Integer> {

        private final String mApiToken;
        private final String mUrl;
        private final String mMethod;
        private String responseBody;

        RoomConfirmTask(String apiToken,  String url, String method)
        {
            mApiToken = apiToken;
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
                jsonObj.put("api_token", mApiToken);
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
                con.connect();

                status = con.getResponseCode();

                bufStr = new BufferedReader(new InputStreamReader(con.getInputStream()));
                responseBody = bufStr.readLine();
                if (HttpURLConnection.HTTP_OK == status)
                {
                    JSONObject tokenJson = new JSONObject(responseBody);
                    teacherName = tokenJson.getString("teacher");
                    lectureName = tokenJson.getString("lecture");
                    timeSlot = tokenJson.getString("timeslot");
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

            return status;
        }

        @Override
        protected void onPostExecute(final Integer status) {
            mRoomConfirmTask = null;
            if (HttpURLConnection.HTTP_OK == status)
            {

                AlertDialog.Builder confirmAlert = new AlertDialog.Builder(selfClass);
                //ダイアログタイトルをセット
                confirmAlert.setTitle(getString(R.string.text_confirm_room));
                //ダイアログメッセージをセット
                confirmAlert.setMessage(teacherName+"\n"+lectureName+"\n"+timeSlot);

                // アラートダイアログのボタンがクリックされた時に呼び出されるコールバックリスナーを登録します
                confirmAlert.setPositiveButton(getString(R.string.action_cancel), new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                        showProgress(false);
                    }});

                // アラートダイアログのボタンがクリックされた時に呼び出されるコールバックリスナーを登録します
                confirmAlert.setNegativeButton(getString(R.string.action_room_in), new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                        final String roomInUrl = getString(R.string.domain) + "/student/rooms/"+roomKey+"?api_token="+mApiToken;
                        mRoomInTask = new RoomInTask(mApiToken,  roomInUrl, "POST");
                        mRoomInTask.execute((Void) null);
                    }});
                //ダイアログ表示
                confirmAlert.show();
            }
            else
            {
                showProgress(false);
                if (400 == status)
                    mRoomKeyView.setError(responseBody);
                else
                    mRoomKeyView.setError(getString(R.string.error_room_key));
                mRoomKeyView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mRoomConfirmTask = null;
            showProgress(false);
        }
    }

    /**
     * Represents an asynchronous room check task used to authenticate
     * the user.
     */
    public class RoomInTask extends AsyncTask<Void, Void, Integer> {

        private final String mApiToken;
        private final String mUrl;
        private final String mMethod;
        private String responseBody;

        RoomInTask(String apiToken,  String url, String method)
        {
            mApiToken = apiToken;
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
                jsonObj.put("action", 1);
                jsonObj.put("type", 1);
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
            mRoomInTask = null;
            showProgress(false);
            if (HttpURLConnection.HTTP_OK == status)
            {

                Intent intent = new Intent(selfClass, ReactionActivity.class);
                intent.putExtra("ROOM_KEY", roomKey);
                intent.putExtra("TEACHER_NAME", teacherName);
                intent.putExtra("LECTURE_NAME", lectureName);
                intent.putExtra("TIME_SLOT", timeSlot);
                startActivity(intent);
            }
            else
            {
                showProgress(false);
                if (400 == status)
                    mRoomKeyView.setError(responseBody);
                else
                    mRoomKeyView.setError(getString(R.string.error_room_key));
                mRoomKeyView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mRoomInTask = null;
            showProgress(false);
        }
    }
}
