package com.tufusi.demo;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.tufusi.demo.event.PersonEvent;
import com.tufusi.xeventbus.XEventBus;

public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
    }

    public void send(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                XEventBus.getDefault().post(new PersonEvent("LeoCheung", 3));
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
