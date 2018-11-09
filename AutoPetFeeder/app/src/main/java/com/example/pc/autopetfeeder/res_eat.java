package com.example.pc.autopetfeeder;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class res_eat extends AppCompatActivity implements View.OnClickListener {
    ImageButton btn_set;

    EditText edt_feed, edt_hour, edt_min;
    Button btn_change, btn_del;
    TextView tv_setTime, tv_si, tv_bun, tv_bowl;
    LinearLayout linearDBtime, linearSet;
    int get_feed;
    Boolean tset = false;
    Boolean set = false;
    Boolean setting = false;
    private static int ONE_MINUTE = 5626;

    AlarmDB myAlarmDB;            // db생성과 테이블생성
    SQLiteDatabase sqlDB;  // 추가, 삭제, 수정, 조회 기능 활용할 데이터베이스
    String strHour, strMin, strGram;

    BluetoothAdapter btAdapter;   //블루투스 연결에 필요한 어댑터 클래스
    static final int REQUEST_ENABLE_BT = 10;  //블루투스 신호를 받아주는 상수값 10
    int mPairedDeviceCount = 0;
    Set<BluetoothDevice> mDevices;  //찾은 블루투스를 세팅하는 변수
    BluetoothDevice mRemoteDevice;   //선택한 블루투스를 넣을 변수
    BluetoothSocket mSocket = null;
    OutputStream moutputStream;
    InputStream minputStream;
    Thread mWorkerThread = null;
    String mStrDelimiter = "\n"; //전송의 끝을 나타냄
    char mCharDelimiter = '\n';
    byte readBuffer[];   //읽은 데이터를 버퍼에 저장
    int readBufferPosition;

    String rstr;

    void beginListenForData() {   //데이터 수신 준비 및 처리 메소드
        final Handler handler = new Handler();
        readBuffer = new byte[1024]; //데이터 수신 버퍼
        readBufferPosition = 0;  //버퍼 내 수신 문자 저장 위치를 지정
        //문자열 수신 쓰레드
        mWorkerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {  //예외가 발생하지 않았을 때
                    try {
                        int bytesAvailable = minputStream.available();  //수신 데이터 확인
                        if (bytesAvailable > 0) { //수신된 데이터가 있는 경우
                            byte[] packetBytes = new byte[bytesAvailable];
                            minputStream.read(packetBytes);  //데이터 읽기
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == mCharDelimiter) {
                                    final byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "UTF-8");  //데이터를 아스키코드값으로 변환
                                    readBufferPosition = 0;  //한줄씩 읽음
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            //수신된 문자열에 대한 작업 처리
                                            tv_bowl.setText(data + mStrDelimiter);

                                        }
                                    });

                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }

                    } catch (IOException e) { //데이터 수신 중 오류 발생
                        Toast.makeText(getApplicationContext(), "수신오류", Toast.LENGTH_SHORT).show();

                    }

                }
            }
        });
        mWorkerThread.start();

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_res_eat);
        android.support.v7.app.ActionBar bar = getSupportActionBar();
        bar.setTitle("예약 냠냠");
        bar.setIcon(R.drawable.ccat);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xff1abc9c));

        Intent intent = getIntent();
        final NotificationManager notificationManager = (NotificationManager) res_eat.this.getSystemService(res_eat.this.NOTIFICATION_SERVICE);

        myAlarmDB = new AlarmDB(this);

        btn_set = (ImageButton) findViewById(R.id.btn_set);
        btn_change = (Button) findViewById(R.id.btn_change);
        btn_del = (Button) findViewById(R.id.btn_del);

        edt_hour = (EditText) findViewById(R.id.edt_hour);
        edt_min = (EditText) findViewById(R.id.edt_min);
        edt_feed = (EditText) findViewById(R.id.edt_feed);

        tv_bowl=(TextView) findViewById(R.id.tv_bowl);
        tv_si = (TextView) findViewById(R.id.tv_si);
        tv_bun = (TextView) findViewById(R.id.tv_bun);
        tv_setTime = (TextView) findViewById(R.id.tv_setTime);

        linearDBtime =(LinearLayout)findViewById(R.id.linearDBtime);
        linearSet =(LinearLayout)findViewById(R.id.linearSet);

        btn_set.setOnClickListener(this);
        btn_change.setOnClickListener(this);
        btn_del.setOnClickListener(this);

        get_feed = intent.getExtras().getInt("feed");
        edt_feed.setText(get_feed + "");

        checkBlueTooth();

        sqlDB = myAlarmDB.getReadableDatabase();                // sqlDB에 읽는부분!
        Cursor cursor;
        cursor = sqlDB.rawQuery("SELECT * FROM setAlarm;", null);                              //레코드의 위치를 선택하기 위해 cursor를 사용하여 쿼리 가져옴 , 여기서는 셀렉션아규먼트 사용안함
        //데이터베이스에서는 while문을 사용
        int length = cursor.getCount();    //이상하게 alreadyCur!=null로 조건을 검색하면 바로밑 else문이 deadcode가 된다
        btn_set.setVisibility(View.VISIBLE);
        btn_change.setVisibility(View.INVISIBLE);
        btn_del.setVisibility(View.INVISIBLE);

        if (length != 0) {//데이터가 없다			//그래서 getcount로 db검색
            while (cursor.moveToNext()) {
                strHour = cursor.getString(0);                  // columnIndex 열번호 (필드인덱스)
                strMin = cursor.getString(1);                //  columnIndex 열번호 (필드인덱스)
                strGram = cursor.getString(2);
            }
            tv_setTime.setText(strHour + "시" + strMin + "분" + strGram + "g");
            edt_hour.setVisibility(View.INVISIBLE);
            edt_min.setVisibility(View.INVISIBLE);
            btn_set.setVisibility(View.INVISIBLE);
            btn_change.setVisibility(View.VISIBLE);
            btn_del.setVisibility(View.VISIBLE);
            tv_si.setVisibility(View.INVISIBLE);
            tv_bun.setVisibility(View.INVISIBLE);
        }
        cursor.close();
        sqlDB.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_set:
                linearDBtime.setVisibility(View.VISIBLE);
                linearSet.setVisibility(View.INVISIBLE);
                btn_change.setVisibility(View.VISIBLE);
                btn_del.setVisibility(View.VISIBLE);
                sqlDB = myAlarmDB.getWritableDatabase();             // sqlDB에 읽고쓴부분에 넣어
                strHour = edt_hour.getText().toString();
                strMin = edt_min.getText().toString();
                strGram = edt_feed.getText().toString();
                sqlDB.execSQL("INSERT INTO setAlarm VALUES('" + strHour + "', '" + strMin + "','" + strGram + "');"); //edt로 입력한 내용을 레코드 생성하여 넣음
                tv_setTime.setText(strHour + "시" + strMin + "분" + strGram + "g");
                sqlDB.close();
                btn_set.setVisibility(View.INVISIBLE);
                new AlarmHATT(getApplicationContext()).Alarm();

                rstr=strGram+strHour+strMin;
                if(moutputStream!=null) sendData(rstr);
                Toast.makeText(getApplicationContext(),"예약 완료!",Toast.LENGTH_LONG).show();
                edt_feed.setText("");

                break;

            case R.id.btn_change:
                if (set == false) {
                    tset = false;
                    btn_change.setText("수정완료");
                    linearDBtime.setVisibility(View.INVISIBLE);
                    linearSet.setVisibility(View.VISIBLE);
                    btn_set.setVisibility(View.INVISIBLE);
                    new AlarmHATT(getApplicationContext()).AlarmCancel();
                    set = true;
                } else {
                    tset = true;
                    btn_change.setText("수정");
                    linearDBtime.setVisibility(View.VISIBLE);
                    linearSet.setVisibility(View.INVISIBLE);
                    sqlDB = myAlarmDB.getWritableDatabase();
                    strHour = edt_hour.getText().toString();
                    strMin = edt_min.getText().toString();
                    strGram = edt_feed.getText().toString();
                    sqlDB.execSQL("UPDATE setAlarm SET hour='" + strHour + "',min='" + strMin + "',gram='" + strGram + "';");
                    tv_setTime.setText(strHour + "시" + strMin + "분" + strGram + "g");
                    new AlarmHATT(getApplicationContext()).Alarm();
                    sqlDB.close();
                    set = false;

                    rstr=strGram+strHour+strMin;
                    if(moutputStream!=null) sendData(rstr);
                    edt_feed.setText("");
                    Toast.makeText(getApplicationContext(),"예약 수정 완료!",Toast.LENGTH_LONG).show();

                }

                break;

            case R.id.btn_del:
                tv_setTime.setText("");
                sqlDB = myAlarmDB.getReadableDatabase();
                sqlDB.execSQL("DELETE FROM setAlarm;");
                sqlDB.close();
                linearDBtime.setVisibility(View.INVISIBLE);
                linearSet.setVisibility(View.VISIBLE);
                btn_set.setVisibility(View.VISIBLE);
                btn_change.setVisibility(View.INVISIBLE);
                btn_del.setVisibility(View.INVISIBLE);
                setting = true;
                new AlarmHATT(getApplicationContext()).AlarmCancel();
                break;

        }
    }



        public class AlarmHATT {
            private Context context;
            AlarmManager am = (AlarmManager) getSystemService(context.ALARM_SERVICE);

            public AlarmHATT(Context context) {
                this.context = context;
            }
            public void Alarm() {

                Intent intent = new Intent(res_eat.this, BroadcastD.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent sender = PendingIntent.getBroadcast(res_eat.this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

                Calendar calendar = Calendar.getInstance();
                //알람시간 calendar에 set해주기

                calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE),Integer.parseInt(strHour) , Integer.parseInt(strMin), 0);

                //알람 예약

                am.set(AlarmManager.RTC, calendar.getTimeInMillis(), sender);
            }

            public void AlarmCancel() {
                Intent intent = new Intent(res_eat.this, BroadcastD.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent sender = PendingIntent.getBroadcast(res_eat.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                am.cancel(sender);
            }

        }

    public class AlarmDB extends SQLiteOpenHelper {

        //생성자로 DB 만듬
        public AlarmDB(Context context) {
            super(context, "alarmDB", null, 1);         // DB이름 설정,심볼?, 버전번호
        }

        //onCreate(): 테이블 생성  /  SQLiteDatabase는 DB를 받는 파라미터변수로 사용
        @Override
        public void onCreate(SQLiteDatabase db) {
            //테이블을 만들기
            db.execSQL("CREATE TABLE setAlarm(hour text, min text, gram text);");
        }

        // onUpgrade() : 테이블 삭제 후 다시 생성
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS setAlarm");                              // 만약 해당 테이블명이 있다면 삭제하겠다
            onCreate(db);                                                              // 이 부분은 테이블을 다시 생성 , 이 부분이 없으면 테이블이 없으므로 레코드를 만들수없음
        }
    }

    void checkBlueTooth() { //블루투스 유무 체크 메소드
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(getApplicationContext(), "블루투스 장치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
        } else {
            if (!btAdapter.isEnabled()) {  //블루투스 연결이 안되었을때
                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);   //인텐트 통해 블루투스연결 작업
                startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);  //블루투스 장치 권한 요청
            } else {
                selectDevice();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { //블루투스 연결 허용
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    selectDevice();
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(getApplicationContext(), "블루투스 권한 요청을 거절했습니다.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //블루투스 장치 선택
    void selectDevice() {
        mDevices = btAdapter.getBondedDevices(); //블루투스 어댑터를 통해 가져온 장치
        mPairedDeviceCount = mDevices.size(); //연결할 블루투스 개수
        if (mPairedDeviceCount == 0) {
            // finish();
            Toast.makeText(getApplicationContext(), "연결할 디바이스가 없습니다.", Toast.LENGTH_SHORT).show();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);  //다이얼로그 대화상자로 리스트를 보여줌
        builder.setTitle("블루투스 장치 선택");
        List<String> listItems = new ArrayList<>();  //선택할 장치들을 동적배열로 보여줌
        for (BluetoothDevice device : mDevices) {
            listItems.add(device.getName());
        }
        listItems.add("취소");
        final CharSequence items[] = listItems.toArray(new CharSequence[listItems.size()]);  //선택한 장치를 배열에 담음
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { //리스트 항목의 위치(포지션)을 나타냄
                if (which == mPairedDeviceCount) {  //취소버튼의 인덱스와 디바이스 개수와 일치
                    //  finish();
                    Toast.makeText(getApplicationContext(), "블루투스 장치 선택을 취소하였습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    connectToSelectedDevice(items[which].toString());
                }
            }
        });
        builder.setCancelable(false); //뒤로가기 버튼 사용 금지
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    void connectToSelectedDevice(String selectedDeviceName) {   //선택한 장치와 연결(페어링 시도)
        mRemoteDevice = getDeviceFromBondedList(selectedDeviceName);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //정해져 있는 블루투스통신 소켓 코드

        try {
            mSocket = mRemoteDevice.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();
            moutputStream = mSocket.getOutputStream();  //데이터 쓸때
            minputStream = mSocket.getInputStream();    //데이터 읽어들일때
            beginListenForData();

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show();   //블루투스 장치끼리 떨어져 있을 때
        }
    }

    BluetoothDevice getDeviceFromBondedList(String name) {  //페어링 작업 목록(연결장치 목록에서 선택할 장치)
        BluetoothDevice selectedDevice = null;
        for (BluetoothDevice device : mDevices) {
            if (name.equals(device.getName())) {
                selectedDevice = device;
            }
        }
        return selectedDevice;
    }

    @Override
    protected void onDestroy() {  //소켓을 닫기 위해 (앱이 종료되면 연결된 모든 장치 close)
        try {
            mWorkerThread.interrupt();
            minputStream.close();
            moutputStream.close();
            mSocket.close();

        } catch (Exception e) {

        }
        super.onDestroy();
    }


    void sendData(String rmsg) {  //데이터 전송 실행 메소드
      //  hm += mStrDelimiter;
       // m += mStrDelimiter;
        rmsg += mStrDelimiter;
        try {
            moutputStream.write(rmsg.getBytes()); //아두이노는 바이트 단위로 보내야함
         //   moutputStream.write(hm.getBytes());
          // moutputStream.write(m.getBytes());
        } catch (Exception e) { //문자열 전송중 에러 발생한 경우
            Toast.makeText(getApplicationContext(), "전송이 되지 않았습니다.", Toast.LENGTH_SHORT).show();
        }
    }

}
