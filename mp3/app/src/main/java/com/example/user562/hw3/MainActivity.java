package com.example.user562.hw3;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import static java.sql.Types.NULL;

public class MainActivity extends AppCompatActivity {


    boolean isPlay = false;

    Intent intentService;
    IMyAidl mBinder = null;

    TextView txtV ;
    Button btn;
    ImageView imgv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //권한체크
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);
        }
        //sd체크
        String ext = Environment.getExternalStorageState();
        if(ext.equals(Environment.MEDIA_MOUNTED) == false) {
            Toast.makeText(this, "SD카드가 없습니다", Toast.LENGTH_LONG).show();
            finish(); return;
        }

        //service 연결
        intentService = new Intent(this,MyService.class);
        bindService(intentService, conn, BIND_AUTO_CREATE);

        txtV = (TextView)findViewById(R.id.txtName);
        btn = (Button)findViewById(R.id.btn2);
        imgv = (ImageView)findViewById(R.id.imgV);
    }


///--------------------    service   --------------------------------///

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = IMyAidl.Stub.asInterface(service);
            try {
                //aidlBack 등록
                mBinder.registerAidlBack(aidlBack);

                //서비스 연결시 재생중인지 알아오기
                isPlay = mBinder.getIsPlaying();
                pauseButton(isPlay);
            } catch (RemoteException e) {
                Log.d("super","onserviceconnected error : " + e.getMessage());
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    // mp3파일명 textview 설정
    IMyAidlBack aidlBack = new IMyAidlBack.Stub() {
        @Override
        public void fileName_fromSerice(String fname) throws RemoteException {
            txtV.setText(fname);
        }
    };


    ////------------------   click    ----------------------------////

    public void onClickPlay(View v) throws RemoteException {
        imgv.setImageResource(R.drawable.im);
        //재생
        if(isPlay){
            isPlay = false;
            mBinder.sendMessage("PLAYPAUSE");
        }
        //일시정지
        else{
            isPlay = true;
            intentService.putExtra("MODE","PLAYSTART");
            startService(intentService);
        }
        pauseButton(isPlay);
    }
    public void onClickBack(View v) throws RemoteException {
        //이전곡
        mBinder.sendMessage("PLAYBACK");
    }
    public void onClickNext(View v) throws RemoteException {
        //다음곡
        mBinder.sendMessage("PLAYNEXT");
    }

    public void pauseButton(boolean flag){
        if(flag == true)
            btn.setText("||");
        else
            btn.setText("▶");
    }



}
