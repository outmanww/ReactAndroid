package com.optimind_react.reactadroid;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // 押した回数のカウンター
    private int clickNum = 0;
    // UI Threadへのpost用ハンドラ
    private Handler mHandler = new Handler();
    // show room key
    private String roomKey = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        roomKey = intent.getStringExtra("ROOM_KEY");//設定したkeyで取り出す

        final Button reactButton = (Button)findViewById(R.id.reactButton);
        reactButton.setOnClickListener(this);
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
}
