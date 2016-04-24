package com.optimind_react.reactadroid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
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

public class MessageActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;


    // show room key
    private static String mRoomKey;
    private static String mApiToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Intent intent = getIntent();
        mRoomKey = intent.getStringExtra("ROOM_KEY");//設定したkeyで取り出す

        React mApp = (React) this.getApplication();
        mApiToken = mApp.getApiToken();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

    }

/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
*/
    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private MessageTask mMsgTask = null;
//        private static final String ARG_HINT = "hint";
        private EditText mMessage;
        private View mProgressView;
        private View mMessageFormView;
        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            /*
            switch (sectionNumber)
            {
                case 0:
                    args.putString(ARG_HINT, fragment.getString(R.string.hint_message_question));
                    break;
                case 1:
                    args.putString(ARG_HINT, fragment.getString(R.string.hint_message_feeling));
                    break;
                case 2:
                    args.putString(ARG_HINT, fragment.getString(R.string.hint_message_others));
                    break;
                default:
                    break;
            }
            args.putInt(ARG_HINT, sectionNumber);
            */
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_message, container, false);
            mProgressView = rootView.findViewById(R.id.messageForm);
            mMessageFormView = rootView.findViewById(R.id.progressBar);
            mMessage = (EditText) rootView.findViewById(R.id.message);
//            mMessage.setHint(getArguments().getString(ARG_HINT));
            Button sendButton = (Button) rootView.findViewById(R.id.sendButton);
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    attemptSendMessage();
                }
            });
            return rootView;
        }

        private void attemptSendMessage()
        {
            if (mMsgTask != null) {
                return;
            }

            // Reset errors.
            mMessage.setError(null);

            // Store values at the time of the attempt.
            String msgContent = mMessage.getText().toString();
            // Check for a valid password, if the user entered one.
            if (TextUtils.isEmpty(msgContent)) {
                mMessage.setError(getString(R.string.error_empty_message));
                mMessage.requestFocus();
            }
            else
            {
                // Show a progress spinner, and kick off a background task to
                // perform the user login attempt.
                showProgress(true);
                final String url = getString(R.string.domain) + "/student/rooms/"+mRoomKey+"?api_token="+mApiToken;
                mMsgTask = new MessageTask(getArguments().getInt(ARG_SECTION_NUMBER), mMessage.getText().toString(), url, "POST");
                mMsgTask.execute((Void) null);
            }
        }
        /**
         * Shows the progress UI and hides the login form.
         */
        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
        private void showProgress(final boolean show) {
            // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
            // for very easy animations. If available, use these APIs to fade-in
            // the progress spinner.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

                mMessageFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                mMessageFormView.animate().setDuration(shortAnimTime).alpha(
                        show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mMessageFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
                mMessageFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        }

        /**
         * Represents an asynchronous room check task used to authenticate
         * the user.
         */
        private class MessageTask extends AsyncTask<Void, Void, Integer> {

            private final String mMessage;
            private final String mUrl;
            private final String mMethod;
            private final Integer mType;
            private String responseBody;

            MessageTask(Integer type, String message, String url, String method)
            {
                mType = type;
                mMessage = message;
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
                    jsonObj.put("action", getResources().getInteger(R.integer.action_message));
                    jsonObj.put("type", mType);
                    jsonObj.put("message", mMessage);
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
                    con.setConnectTimeout(getResources().getInteger(R.integer.delay_http_connect));
                    con.setReadTimeout(getResources().getInteger(R.integer.delay_http_read));

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
                mMsgTask = null;
                showProgress(false);

                AlertDialog.Builder alertQuitRoom = new AlertDialog.Builder(getActivity());
                //ダイアログタイトルをセット
                alertQuitRoom.setTitle(getString(R.string.title_finish_message_task));
                //ダイアログメッセージをセット
                alertQuitRoom.setMessage(null);
                alertQuitRoom.setCancelable(false);
                // ボタンがクリックされた時に呼び出されるコールバックリスナーを登録します
                alertQuitRoom.setPositiveButton("OK",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                        //退室処理
                        getActivity().finish();
                    }});
                //ダイアログ表示
                alertQuitRoom.show();
            }

            @Override
            protected void onCancelled() {
                mMsgTask = null;
                showProgress(false);
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.tab_question);
                case 1:
                    return getString(R.string.tab_feeling);
                case 2:
                    return getString(R.string.tab_others);
            }
            return null;
        }
    }
}
