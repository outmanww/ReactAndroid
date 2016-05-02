package com.optimind_react.reactadroid;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class LicenseAgreementActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license_agreement);

        final Button agreeButton = (Button)findViewById(R.id.agree_button);

        if(agreeButton != null)
            agreeButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    Intent intent = new Intent(LicenseAgreementActivity.this, RegisterActivity.class);
                    startActivity(intent);
                }
            });
    }
}
