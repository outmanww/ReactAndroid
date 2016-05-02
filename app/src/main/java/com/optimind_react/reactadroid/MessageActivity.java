package com.optimind_react.reactadroid;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.support.design.widget.TabLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MessageActivity extends AppCompatActivity
{
    // tag for debug
    private final static String TAG = MessageActivity.class.getSimpleName();
    // room key
    private static String mRoomKey;
    private static String mApiToken;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Intent intent = getIntent();
        mRoomKey = intent.getStringExtra("ROOM_KEY");//設定したkeyで取り出す

        React mApp = (React) this.getApplication();
        mApiToken = mApp.getApiToken();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /**
         * The {@link android.support.v4.view.PagerAdapter} that will provide
         * fragments for each of the sections. We use a
         * {@link FragmentPagerAdapter} derivative, which will keep every
         * loaded fragment in memory. If this becomes too memory intensive, it
         * may be best to switch to a
         * {@link android.support.v4.app.FragmentStatePagerAdapter}.
         */
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        /**
         * The {@link ViewPager} that will host the section contents.
         */
        // Set up the ViewPager with the sections adapter.
        ViewPager viewPager = (ViewPager) findViewById(R.id.container);
        if(viewPager != null)
            viewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        if(tabLayout != null)
            tabLayout.setupWithViewPager(viewPager);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            Window window = getWindow();

            // clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // finally change the color
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorBack));
        }
    }

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
        final String url = getString(R.string.domain) + "/student/rooms/"+mRoomKey+"?api_token="+mApiToken;
        mSectionsPagerAdapter.newReactTask(getResources().getInteger(R.integer.action_basic), getResources().getInteger(R.integer.type_fore_out), null, url);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        // happened by the app itself
        React mApp = (React) this.getApplication();
        if(mApp.getAppStatus() != React.AppStatus.RETURNED_TO_FOREGROUND)
            return;

        // start by screen on
        if(!mApp.getScreenStatus())
            return;

        Log.d(TAG, "onStart");
        final String url = getString(R.string.domain) + "/student/rooms/"+mRoomKey+"?api_token="+mApiToken;
        mSectionsPagerAdapter.newReactTask(getResources().getInteger(R.integer.action_basic), getResources().getInteger(R.integer.type_fore_in), null, url);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public ReactTask mReactTask = null;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void newReactTask(Integer action, Integer type, String message, String url)
        {
            mReactTask = new ReactTask(action, type, message, url);
            mReactTask.execute((Void) null);
        }

        /**
         * Represents an asynchronous message send task
         */
        private class ReactTask extends AsyncTask<Void, Void, JSONObject>
        {
            private final String mMessage;
            private final String mUrl;
            private final Integer mAction;
            private final Integer mType;
            private Integer mResponseCode = 0;
            private boolean isTimeOut = false;

            ReactTask(Integer action, Integer type, String message, String url)
            {
                mAction = action;
                mType = type;
                mMessage = message;
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

                try {
                    JSONObject jsonInput = new JSONObject();
                    jsonInput.put("action", mAction);
                    jsonInput.put("type", mType);
                    jsonInput.put("message", mMessage);

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
                    if (mAction == getResources().getInteger(R.integer.action_message))
                    {
                        AlertDialog.Builder alertQuitRoom = new AlertDialog.Builder(MessageActivity.this);
                        alertQuitRoom.setTitle(getString(R.string.title_finish_message_task));
                        alertQuitRoom.setMessage(null);
                        alertQuitRoom.setCancelable(false);
                        alertQuitRoom.setPositiveButton(getString(R.string.action_ok),new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which) {
                                // quit room
                                finish();
                            }});
                        // show dialog
                        alertQuitRoom.show();
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
                    final ViewPager layout = (ViewPager) findViewById(R.id.container);
                    if(layout != null)
                        Snackbar.make(layout, errMsg, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            protected void onCancelled()
            {
                mReactTask = null;
                //showProgress(false);
            }
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1, getResources(), this);
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
    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment
    {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static final String ARG_HINT = "hint";
        private EditText mMessage, mName;
        private CheckBox mAnonymous;
        private SectionsPagerAdapter mSecPageAdapter;

        public PlaceholderFragment()
        {
        }

        public void setSectionsPagerAdapter(SectionsPagerAdapter parentInstance)
        {
            mSecPageAdapter = parentInstance;
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber, Resources res, SectionsPagerAdapter parentInstance) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            fragment.setSectionsPagerAdapter(parentInstance);
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);

            switch (sectionNumber)
            {
                case 1:
                    args.putString(ARG_HINT, res.getString(R.string.hint_message_question));
                    break;
                case 2:
                    args.putString(ARG_HINT, res.getString(R.string.hint_message_feeling));
                    break;
                case 3:
                    args.putString(ARG_HINT, res.getString(R.string.hint_message_others));
                    break;
                default:
                    break;
            }

            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_message, container, false);
            mMessage = (EditText) rootView.findViewById(R.id.message);
            mMessage.setHint(getArguments().getString(ARG_HINT));
            Integer color = Color.parseColor("#FFFFFFFF");
            mMessage.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

            mName = (EditText) rootView.findViewById(R.id.name);
            mName.setVisibility(View.INVISIBLE);
            React app = (React) getActivity().getApplication();
            mName.setText(app.getName());
            mAnonymous = (CheckBox) rootView.findViewById(R.id.anonymous);
            mAnonymous.setChecked(true);
            mAnonymous.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mAnonymous.isChecked())
                        mName.setVisibility(View.INVISIBLE);
                    else
                        mName.setVisibility(View.VISIBLE);
                }
            });

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
            if (mSecPageAdapter.mReactTask != null) {
                return;
            }

            // Reset errors.
            mMessage.setError(null);

            // Store values at the time of the attempt.
            String name = mName.getText().toString();
            String content = mMessage.getText().toString();
            if (TextUtils.isEmpty(content)) {
                mMessage.setError(getString(R.string.error_empty_message));
                mMessage.requestFocus();
                return;
            }

            String msg = "";
            if(!mAnonymous.isChecked() && !name.equals("")) {
                msg += "[" + mName.getText().toString() + "]: ";
                React app = (React) getActivity().getApplication();
                app.setName(mName.getText().toString());
            }
            msg += content;

            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
//            showProgress(true);
            final String url = getString(R.string.domain) + "/student/rooms/"+mRoomKey+"?api_token="+mApiToken;
            mSecPageAdapter.newReactTask(getResources().getInteger(R.integer.action_message), getArguments().getInt(ARG_SECTION_NUMBER), msg, url);
        }

        /**
         * Shows the progress UI and hides the login form.
         */
        /*
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
        */
    }
}
