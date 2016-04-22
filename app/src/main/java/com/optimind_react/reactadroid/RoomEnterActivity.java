package com.optimind_react.reactadroid;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.text.InputType;
public class RoomEnterActivity extends AppCompatActivity implements View.OnClickListener {

    private String keyValue="123456";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_enter);

        final Button roomKeyButton = (Button)findViewById(R.id.roomKeyButton);
        roomKeyButton.setOnClickListener(this);

        EditText roomKeyText = (EditText)findViewById(R.id.roomKeyText);
        //パスワード入力
        roomKeyText.setInputType(InputType.TYPE_CLASS_NUMBER);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.roomKeyButton:
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("ROOM_KEY", keyValue);
                startActivity(intent);
                break;

            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // log out
    }
}
