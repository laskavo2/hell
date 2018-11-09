package com.example.pc.autopetfeeder;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

public class dog extends AppCompatActivity implements View.OnClickListener{
    Button btn_now, btn_res;
    Intent nintent, lintent;
    Spinner mSpin;
    String dogweigth[] = {"1kg","1.5kg","2kg","3kg","4kg","5kg","6kg","7kg","8kg","9kg","10kg"};
    Integer dogfeed[] = {45,55,65,86,105,120,130,143,160,170,190};
    ArrayAdapter adapter;
    TextView tv_feed;
    Integer recom_feed;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dog);
        android.support.v7.app.ActionBar bar = getSupportActionBar();
        bar.setIcon(R.drawable.ccat);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xff1abc9c));
        bar.setTitle("우리 댕댕이 밥주기");

        btn_now = (Button) findViewById(R.id.btn_now);
        btn_res = (Button) findViewById(R.id.btn_res);
        mSpin = (Spinner) findViewById(R.id.spinner);
        tv_feed = (TextView) findViewById(R.id.tv_feed);
        btn_res.setOnClickListener(this);
        btn_now.setOnClickListener(this);

        adapter=new ArrayAdapter(this,R.layout.support_simple_spinner_dropdown_item,dogweigth);
        mSpin.setAdapter(adapter);

        mSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tv_feed.setText(dogfeed[position]+"g");
                recom_feed = dogfeed[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_now:
                nintent = new Intent(this,now_eat.class);
                nintent.putExtra("feed",recom_feed);
                startActivity(nintent);
                break;
            case R.id.btn_res:
                lintent = new Intent(this,res_eat.class);
                lintent.putExtra("feed",recom_feed);
                startActivity(lintent);
                break;
        }
    }
}
