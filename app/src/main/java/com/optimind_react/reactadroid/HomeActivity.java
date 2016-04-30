package com.optimind_react.reactadroid;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class HomeActivity extends Activity implements View.OnClickListener
{
    private final int REQUEST_PERMISSION = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        final Button lectureButton = (Button)findViewById(R.id.lectureButton);
        lectureButton.setOnClickListener(this);
    }

    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.lectureButton:
                if(Build.VERSION.SDK_INT >= 23)
                    checkPermission();
                else
                    transitRoomLayout();
                break;
            default:
                break;
        }
    }

    private void transitRoomLayout()
    {
        Intent intent = new Intent(this, RoomEnterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    // confirm the permission of using gps
    private void checkPermission()
    {
        // already permitted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED)
            transitRoomLayout();
        else
            requireLocationPermission();
    }

    // require
    private void requireLocationPermission()
    {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
            ActivityCompat.requestPermissions(HomeActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION);
        else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, REQUEST_PERMISSION);
    }

    // 結果の受け取り
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_PERMISSION)
        {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                transitRoomLayout();
            }
            else
            {
                // それでも拒否された時の対応
                final LinearLayout layout = (LinearLayout) findViewById(R.id.root_layout);
                Snackbar.make(layout, R.string.error_gps_needed, Snackbar.LENGTH_LONG).show();
            }
        }
    }
}
