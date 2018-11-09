package com.example.pc.autopetfeeder;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Bluetooth extends AppCompatActivity {

    Button btn_bt, btn_dog, btn_cat;
    BluetoothAdapter btAdapter;   //블루투스 연결에 필요한 어댑터 클래스
    static final int REQUEST_ENABLE_BT=10;  //블루투스 신호를 받아주는 상수값 10
    int mPairedDeviceCount=0;
    Set<BluetoothDevice> mDevices;  //찾은 블루투스를 세팅하는 변수
    BluetoothDevice mRemoteDevice;   //선택한 블루투스를 넣을 변수
    BluetoothSocket mSocket=null;
    OutputStream moutputStream;
    InputStream minputStream;
    Thread mWorkerThread=null;
    String mStrDelimiter="\n"; //전송의 끝을 나타냄
    char mCharDelimiter='\n';
    byte readBuffer[];   //읽은 데이터를 버퍼에 저장
    int readBufferPosition;


    String str;



    void checkBlueTooth(){ //블루투스 유무 체크 메소드
        btAdapter=BluetoothAdapter.getDefaultAdapter();
        if(btAdapter==null){
            //   finish();
            Toast.makeText(getApplicationContext(),"블루투스 장치를 찾을 수 없습니다.",Toast.LENGTH_SHORT).show();
        } else {
            if(!btAdapter.isEnabled()){  //블루투스 연결이 안되었을때
                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);   //인텐트 통해 블루투스연결 작업
                startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);  //블루투스 장치 권한 요청
            }else {
                selectDevice();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { //블루투스 연결 허용
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode==RESULT_OK){
                    selectDevice();
                } else if(resultCode==RESULT_CANCELED){
                    //finish();
                    Toast.makeText(getApplicationContext(),"블루투스 권한 요청을 거절했습니다.",Toast.LENGTH_SHORT).show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    //블루투스 장치 선택
    void selectDevice() {
        mDevices=btAdapter.getBondedDevices(); //블루투스 어댑터를 통해 가져온 장치
        mPairedDeviceCount=mDevices.size(); //연결할 블루투스 개수
        if (mPairedDeviceCount==0){
            // finish();
            Toast.makeText(getApplicationContext(),"연결할 디바이스가 없습니다.",Toast.LENGTH_SHORT).show();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);  //다이얼로그 대화상자로 리스트를 보여줌
        builder.setTitle("블루투스 장치 선택");
        List<String> listItems = new ArrayList<>();  //선택할 장치들을 동적배열로 보여줌
        for(BluetoothDevice device:mDevices){
            listItems.add(device.getName());
        }
        listItems.add("취소");
        final CharSequence items[]=listItems.toArray(new CharSequence[listItems.size()]);  //선택한 장치를 배열에 담음
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { //리스트 항목의 위치(포지션)을 나타냄
                if(which==mPairedDeviceCount){  //취소버튼의 인덱스와 디바이스 개수와 일치
                    //  finish();
                    Toast.makeText(getApplicationContext(),"블루투스 장치 선택을 취소하였습니다.",Toast.LENGTH_SHORT).show();
                }else {
                    connectToSelectedDevice(items[which].toString());
                }
            }
        });
        builder.setCancelable(false); //뒤로가기 버튼 사용 금지
        AlertDialog alertDialog=builder.create();
        alertDialog.show();
    }

    void connectToSelectedDevice(String selectedDeviceName) {   //선택한 장치와 연결(페어링 시도)
        mRemoteDevice=getDeviceFromBondedList(selectedDeviceName);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //정해져 있는 블루투스통신 소켓 코드

        try {
            mSocket=mRemoteDevice.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();
            moutputStream=mSocket.getOutputStream();  //데이터 쓸때
            minputStream=mSocket.getInputStream();    //데이터 읽어들일때
            beginListenForData();

        }catch (Exception e){
            Toast.makeText(getApplicationContext(),"연결이 끊어졌습니다.",Toast.LENGTH_SHORT).show();   //블루투스 장치끼리 떨어져 있을 때
        }
    }

    BluetoothDevice getDeviceFromBondedList(String name) {  //페어링 작업 목록(연결장치 목록에서 선택할 장치)
        BluetoothDevice selectedDevice=null;
        for(BluetoothDevice device : mDevices){
            if(name.equals(device.getName())){
                selectedDevice=device;
            }
        }
        return selectedDevice ;
    }

    @Override
    protected void onDestroy() {  //소켓을 닫기 위해 (앱이 종료되면 연결된 모든 장치 close)
        try{
            mWorkerThread.interrupt();
            minputStream.close();
            moutputStream.close();
            mSocket.close();

        } catch (Exception e) {

        }
        super.onDestroy();
    }

    void beginListenForData() {   //데이터 수신 준비 및 처리 메소드
        final Handler handler=new Handler();
        readBuffer=new byte[1024]; //데이터 수신 버퍼
        readBufferPosition=0;  //버퍼 내 수신 문자 저장 위치를 지정
        //문자열 수신 쓰레드
        mWorkerThread=new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()){  //예외가 발생하지 않았을 때
                    try {
                        int bytesAvailable=minputStream.available();  //수신 데이터 확인
                        if(bytesAvailable>0) { //수신된 데이터가 있는 경우
                            byte[] packetBytes=new byte[bytesAvailable];
                            minputStream.read(packetBytes);  //데이터 읽기
                            for(int i=0; i<bytesAvailable; i++){
                                byte b=packetBytes[i];
                                if(b==mCharDelimiter){
                                    final byte[] encodedBytes=new byte[readBufferPosition];
                                    System.arraycopy(readBuffer,0,encodedBytes,0, encodedBytes.length);
                                    final String data=new String(encodedBytes,"US-ASCII");  //데이터를 아스키코드값으로 변환
                                    readBufferPosition=0;  //한줄씩 읽음
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            //수신된 문자열에 대한 작업 처리



                                        }
                                    });

                                } else {
                                    readBuffer[readBufferPosition++]=b;
                                }
                            }
                        }

                    } catch(IOException e){ //데이터 수신 중 오류 발생

                    }

                }
            }
        });
        mWorkerThread.start();

    }
    void sendData(String msg){  //데이터 전송 실행 메소드
        msg+=mStrDelimiter;
        try{
            moutputStream.write(msg.getBytes()); //아두이노는 바이트 단위로 보내야함
        }catch (Exception e){ //문자열 전송중 에러 발생한 경우
            Toast.makeText(getApplicationContext(),"전송이 되지 않았습니다.",Toast.LENGTH_SHORT).show();
        }
    }
}
