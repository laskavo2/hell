package com.example.pc.autopetfeeder;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class intro extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        ActionBar ab = getSupportActionBar();
        ab.hide();

     /*   View decorView = getWindow().getDecorView();
        int uiOption = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOption);*/

        Thread timerThread = new Thread(){
            public  void  run(){
                try{
                    sleep(2500);
                } catch (InterruptedException e){
                    e.printStackTrace();
                } finally {
                    Intent intent;
                    intent = new Intent(intro.this,MainActivity.class);
                    startActivity(intent);
                }
            }
        };
        timerThread.start();


    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (false){
            super.onBackPressed();
        }else {

        }

    }
}
