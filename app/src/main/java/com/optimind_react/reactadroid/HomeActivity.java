package com.optimind_react.reactadroid;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    private final int REQUEST_PERMISSION = 10;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        final ImageButton lectureButton = (ImageButton)findViewById(R.id.lectureButton);
        lectureButton.setOnClickListener(this);
        final ImageButton PointButton = (ImageButton)findViewById(R.id.pointButton);
        PointButton.setOnClickListener(this);
        getSupportActionBar().hide();
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.lectureButton:
            case R.id.pointButton:
                if(Build.VERSION.SDK_INT >= 23){
                    checkPermission();
                }
                else{
                    transitRoomLayout();
                }
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

    // 位置情報許可の確認
    private void checkPermission() {
        // 既に許可している
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            transitRoomLayout();
        }
        // 拒否していた場合
        else{
            requestLocationPermission();
        }
    }
    // 許可を求める
    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(HomeActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION);

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, REQUEST_PERMISSION);
        }
    }

    // 結果の受け取り
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                transitRoomLayout();
                return;

            } else {
                // それでも拒否された時の対応
                Toast toast = Toast.makeText(this, getString(R.string.error_gps_needed), Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }
}
