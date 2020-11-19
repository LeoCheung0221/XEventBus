package com.tufusi.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tufusi.demo.event.PersonEvent;
import com.tufusi.xeventbus.Subscriber;
import com.tufusi.xeventbus.ThreadMode;
import com.tufusi.xeventbus.XEventBus;

public class MainActivity extends AppCompatActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        XEventBus.getDefault().register(this);
        textView = findViewById(R.id.tv);
    }

    public void change(View view) {
        startActivity(new Intent(this, SecondActivity.class));
    }


    @Subscriber(threadMode = ThreadMode.MAIN)
    public void receive(PersonEvent person){
        textView.setText(person.toString());
        Toast.makeText(this, person.toString(), Toast.LENGTH_SHORT).show();
    }

    @Subscriber
    public void receive(String s){
        textView.setText(s);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        XEventBus.getDefault().unregister(this);
    }
}