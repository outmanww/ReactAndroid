package com.optimind_react.reactadroid;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.ImageButton;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    // show room key
    private String roomKey = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        final ImageButton lectureButton = (ImageButton)findViewById(R.id.lectureButton);
        lectureButton.setOnClickListener(this);
        final ImageButton PointButton = (ImageButton)findViewById(R.id.pointButton);
        PointButton.setOnClickListener(this);
    }

    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.lectureButton:
                intent = new Intent(this, RoomEnterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                break;
            case R.id.pointButton:
                intent = new Intent(this, RoomEnterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                break;
            default:
                break;
        }
    }
}
