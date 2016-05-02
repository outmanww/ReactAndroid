package com.optimind_react.reactadroid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A register screen that offers register via name/email/password.
 */
public class RegisterActivity extends AppCompatActivity 
{
    // tag for debug
    private final static String TAG = RegisterActivity.class.getSimpleName();
    
    /**
     * Keep track of the register task to ensure we can cancel it if requested.
     */
    private UserRegisterTask mRegisterTask = null;

    // UI references.
    private EditText mEmailView;
    private EditText mFamilyNameView;
    private EditText mGivenNameView;
    private EditText mPasswordView;
    private EditText mPasswordConfirmView;
    private View mProgressView;
    private View mRegisterFormView;
    private LinearLayout mRootLayout;

    private InputMethodManager mInputMethodManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Set up the register form.
        mFamilyNameView = (EditText) findViewById(R.id.family_name);
        mGivenNameView = (EditText) findViewById(R.id.given_name);
        mEmailView = (EditText) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordConfirmView = (EditText) findViewById(R.id.password2);
        mPasswordConfirmView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.sign_up || id == EditorInfo.IME_NULL) {
                    attemptRegister();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignUpButton = (Button) findViewById(R.id.user_sign_up_button);
        if(mEmailSignUpButton != null)
            mEmailSignUpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        mRegisterFormView = findViewById(R.id.register_form);
        mProgressView = findViewById(R.id.register_progress);
        mRootLayout = (LinearLayout) findViewById(R.id.root_layout);

        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mFamilyNameView.requestFocus();
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
     * Attempts to sign in or register the account specified by the register form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual register attempt is made.
     */
    private void attemptRegister()
    {
        if (mRegisterTask != null)
            return;

        // Reset errors.
        mFamilyNameView.setError(null);
        mGivenNameView.setError(null);
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mPasswordConfirmView.setError(null);

        // Store values at the time of the register attempt.
        String familyName = mFamilyNameView.getText().toString();
        String givenName = mGivenNameView.getText().toString();
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String passwordConfirm = mPasswordConfirmView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password))
        {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }
        else if(!React.isPasswordValid(password))
        {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(passwordConfirm))
        {
            mPasswordConfirmView.setError(getString(R.string.error_field_required));
            focusView = mPasswordConfirmView;
            cancel = true;
        }
        // Check for a valid password, if the user entered one.
        else if (!passwordConfirm.equals(password))
        {
            mPasswordConfirmView.setError(getString(R.string.error_different_password));
            focusView = mPasswordConfirmView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email))
        {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!React.isEmailValid(email))
        {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        // Check for a valid name.
        if (TextUtils.isEmpty(givenName))
        {
            mGivenNameView.setError(getString(R.string.error_field_required));
            focusView = mGivenNameView;
            cancel = true;
        }

        // Check for a valid name.
        if (TextUtils.isEmpty(familyName))
        {
            mFamilyNameView.setError(getString(R.string.error_field_required));
            focusView = mFamilyNameView;
            cancel = true;
        }

        if (cancel)
        {
            // There was an error; don't attempt register and focus the first
            // form field with an error.
            focusView.requestFocus();
        }
        else
        {
            // Show a progress spinner, and kick off a background task to
            // perform the user register attempt.
            showProgress(true);
            final String url = getString(R.string.domain)+"/student/signup";
            mRegisterTask = new UserRegisterTask(familyName, givenName, email, password, url);
            mRegisterTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the register form.
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

            mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mRegisterFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private class UserRegisterTask extends AsyncTask<Void, Void, JSONObject>
    {
        private final String mFamilyName;
        private final String mGivenName;
        private final String mEmail;
        private final String mPassword;
        private final String mUrl;
        private Integer mResponseCode = 0;
        private  boolean isTimeOut = false;

        UserRegisterTask(String familyName, String givenName, String email, String password, String url)
        {
            mFamilyName = familyName;
            mGivenName = givenName;
            mEmail = email;
            mPassword = password;
            mUrl = url;
        }

        @Override
        protected JSONObject doInBackground(Void... params)
        {
            Log.d(TAG, "RegisterTask Start");
            HttpURLConnection con = null;
            URL url;

            JSONObject jsonOutput = null;
            BufferedReader bufStr;

            try
            {
                JSONObject jsonInput = new JSONObject();
                jsonInput.put("family_name", mFamilyName);
                jsonInput.put("given_name", mGivenName);
                jsonInput.put("email", mEmail);
                jsonInput.put("password", mPassword);

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
            Log.d(TAG, "RegisterTask Finish");

            mRegisterTask = null;
            showProgress(false);

            String errMsg = null;

            if (HttpURLConnection.HTTP_OK == mResponseCode)
            {
                React mApp = (React) RegisterActivity.this.getApplication();
                try
                {
                    mApp.setName(mFamilyName+mGivenName);
                    mApp.setEmail(mEmail);
                    mApp.setApiToken(result.getString("api_token"));
                }catch (JSONException e)
                {
                    e.printStackTrace();
                }
                Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
                return;
            }

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
                    switch(info[0])
                    {
                        case "password":
                            mPasswordView.setError(message);
                            mPasswordView.requestFocus();
                            break;
                        case "email":
                            mEmailView.setError(message);
                            mEmailView.requestFocus();
                            break;
                        case "family_name":
                            mFamilyNameView.setError(message);
                            mFamilyNameView.requestFocus();
                            break;
                        case "given_name":
                            mGivenNameView.setError(message);
                            mGivenNameView.requestFocus();
                            break;
                        default:
                            errMsg = message;
                            break;
                    }
                }
                catch (JSONException e)
                {
                    errMsg = getString(R.string.error_unknown);
                }
            }

            if(!TextUtils.isEmpty(errMsg))
            {
                final LinearLayout layout = (LinearLayout) findViewById(R.id.root_layout);
                if(layout != null)
                    Snackbar.make(layout, errMsg, Snackbar.LENGTH_LONG)
                            .setAction(getString(R.string.text_resend), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    attemptRegister();
                                }
                            })
                            .show();
            }
        }

        @Override
        protected void onCancelled()
        {
            mRegisterTask = null;
            showProgress(false);
        }
    }
}

