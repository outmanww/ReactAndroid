package com.optimind_react.reactadroid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.content.Intent;
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
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity
{
    // tag for debug
    private final static String TAG = LoginActivity.class.getSimpleName();

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private LoginTask mLoginTask = null;
    private ResetPasswordTask mResetPasswordTask = null;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private AlertDialog mForgetPwdDialog;
    private EditText mForgotPwdEmailInputView;
    private LinearLayout mRootLayout;

    private InputMethodManager mInputMethodManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mEmailView = (EditText) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        if(mEmailSignInButton != null)
            mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        TextView mRegisterText = (TextView) findViewById(R.id.register_text);
        if(mRegisterText != null)
            mRegisterText.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(LoginActivity.this, LicenseAgreementActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                }
            });

        mForgotPwdEmailInputView = new EditText(LoginActivity.this);
        mForgotPwdEmailInputView.setText(mEmailView.getText().toString());
        mForgotPwdEmailInputView.setSingleLine();
        mForgotPwdEmailInputView.setImeOptions(EditorInfo.IME_ACTION_SEND);
        mForgotPwdEmailInputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_SEND || id == EditorInfo.IME_NULL) {
                    boolean result = attemptResetPwd(mForgotPwdEmailInputView.getText().toString());
                    if(result)
                        mForgetPwdDialog.dismiss();
                    return true;
                }
                return false;
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        mForgetPwdDialog = builder.create();
        mForgetPwdDialog.setIcon(android.R.drawable.ic_dialog_info);
        mForgetPwdDialog.setTitle(getString(R.string.title_find_pwd));
        mForgetPwdDialog.setMessage(getString(R.string.text_input_email));
        mForgetPwdDialog.setView(mForgotPwdEmailInputView);
        mForgetPwdDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.action_ok), new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
            }});
        mForgetPwdDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.action_cancel), new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
            }});

        TextView mForgetPwdText = (TextView) findViewById(R.id.forget_pwd_text);
        if(mForgetPwdText != null)
            mForgetPwdText.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    mForgetPwdDialog.show();

                    mForgetPwdDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View dialog)
                        {
                            boolean rst = attemptResetPwd(mForgotPwdEmailInputView.getText().toString());
                            if(rst)
                                mForgetPwdDialog.dismiss();
                        }});

                    mForgotPwdEmailInputView.requestFocus();
                    mForgotPwdEmailInputView.setText(mEmailView.getText().toString());
                }
            });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        mRootLayout = (LinearLayout) findViewById(R.id.root_layout);

        React app = (React) this.getApplication();
        mEmailView.setText(app.getEmail());

        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mEmailView.requestFocus();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        mInputMethodManager.hideSoftInputFromWindow(mRootLayout.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
        mRootLayout.requestFocus();
        return true;
    }


    /**
     * Attempts to login the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin()
    {
        if (mLoginTask != null)
            return;

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

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

        if (cancel)
        {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        }
        else
        {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            final String url = getString(R.string.domain)+"/student/signin";
            mLoginTask = new LoginTask(email, password, url);
            mLoginTask.execute((Void) null);
        }
    }

    private boolean attemptResetPwd(final String email)
    {
        boolean rst = true;
        // Check for a valid email address.
        if (TextUtils.isEmpty(email))
        {
            mForgotPwdEmailInputView.setError(getString(R.string.error_field_required));
            mForgotPwdEmailInputView.requestFocus();
            rst = false;
        } else if (!React.isEmailValid(email))
        {
            mForgotPwdEmailInputView.setError(getString(R.string.error_invalid_email));
            mForgotPwdEmailInputView.requestFocus();
            rst = false;
        }
        else
        {
            showProgress(true);
            final String url = getString(R.string.domain) + "/student/password/email";
            mResetPasswordTask = new ResetPasswordTask(email, url);
            mResetPasswordTask.execute((Void) null);
        }
        return rst;
    }

    /**
     * Shows the progress UI and hides the login form.
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

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login task used to authenticate
     * the user.
     */
    private class LoginTask extends AsyncTask<Void, Void, JSONObject>
    {
        private final String mEmail;
        private final String mPassword;
        private final String mUrl;
        private Integer mResponseCode = 0;
        private boolean isTimeOut = false;

        LoginTask(String email, String password, String url)
        {
            mEmail = email;
            mPassword = password;
            mUrl = url;
        }

        @Override
        protected JSONObject doInBackground(Void... params)
        {
            Log.d(TAG, "LoginTask Start");
            HttpURLConnection con = null;
            URL url;

            JSONObject jsonOutput = null;
            BufferedReader bufStr;

            try
            {
                JSONObject jsonInput = new JSONObject();
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
            } catch (java.net.UnknownHostException|java.net.SocketTimeoutException e) {
                isTimeOut = true;
                e.printStackTrace();
            } catch (JSONException|IOException e)
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
            Log.d(TAG, "LoginTask Finish");

            mLoginTask = null;
            showProgress(false);

            String errMsg = null;
            if (HttpURLConnection.HTTP_OK == mResponseCode)
            {
                React mApp = (React) LoginActivity.this.getApplication();
                try
                {
                    mApp.setApiToken(result.getString("api_token"));
                }catch (JSONException e)
                {
                    e.printStackTrace();
                }
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
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
                        case "student":
                            mEmailView.setError(message);
                            mEmailView.requestFocus();
                            break;
                        default:
                            errMsg = message;
                            break;
                    }
                }
                catch (JSONException e)
                {
                    errMsg = getString(R.string.error_unknown);
                    e.printStackTrace();
                }
            }

            if(!TextUtils.isEmpty(errMsg))
            {
                if(mRootLayout != null)
                    Snackbar.make(mRootLayout, errMsg, Snackbar.LENGTH_LONG)
                            .setAction(getString(R.string.text_resend), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    attemptLogin();
                                }
                            })
                            .show();
            }
        }

        @Override
        protected void onCancelled()
        {
            mLoginTask = null;
            showProgress(false);
        }
    }

    private class ResetPasswordTask extends AsyncTask<Void, Void, JSONObject>
    {
        private final String mEmail;
        private final String mUrl;
        private Integer mResponseCode = 0;
        private  boolean isTimeOut = false;

        ResetPasswordTask(String email, String url)
        {
            mEmail = email;
            mUrl = url;
        }

        @Override
        protected JSONObject doInBackground(Void... params)
        {
            Log.d(TAG, "ResetPasswordTask Start");
            HttpURLConnection con = null;
            URL url;

            JSONObject jsonOutput = null;
            BufferedReader bufStr;

            try
            {
                JSONObject jsonInput = new JSONObject();
                jsonInput.put("email", mEmail);

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
            Log.d(TAG, "ResetPasswordTask Finish");

            mResetPasswordTask = null;
            showProgress(false);

            String errMsg = null;

            if (HttpURLConnection.HTTP_OK == mResponseCode)
            {
                if(mRootLayout != null)
                    Snackbar.make(mRootLayout, R.string.dialog_password_reset, Snackbar.LENGTH_LONG).show();
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
                    String message = result.getString("message");
                    mForgetPwdDialog.show();
                    mForgotPwdEmailInputView.setError(message);
                    mForgotPwdEmailInputView.requestFocus();
                }
                catch (JSONException e)
                {
                    errMsg = getString(R.string.error_unknown);
                }
            }

            if(!TextUtils.isEmpty(errMsg))
            {
                if(mRootLayout != null)
                    Snackbar.make(mRootLayout, errMsg, Snackbar.LENGTH_LONG)
                        .show();
            }
        }

        @Override
        protected void onCancelled()
        {
            mResetPasswordTask = null;
            showProgress(false);
        }
    }
}