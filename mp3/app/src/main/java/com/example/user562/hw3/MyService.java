package com.example.user562.hw3;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.LoginFilter;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;

/**
 * Created by user562 on 2018-12-06.
 */

public class MyService extends Service {

    MediaPlayer player;
    IMyAidlBack aidlBack;

    BroadcastReceiver receiver1 = null;
    BroadcastReceiver receiver2 = null;
    BroadcastReceiver receiver3 = null;

    String[] mpList ;
    String sdpath;
    int idx =0;
    boolean wasStart = false;


    public void onCreate() {
        super.onCreate();

        player = new MediaPlayer();
        player.setOnCompletionListener(onComplete);
        sdpath = Environment.getExternalStorageDirectory().getAbsolutePath() ;
        handler.sendEmptyMessageDelayed(0,500);

        //receiver등록
        IntentFilter filter1 = new IntentFilter("PLAYSTART");
        receiver1 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String mode;
                if(!player.isPlaying())
                    mode = "PLAYSTART";
                else
                    mode = "PLAYPAUSE";

                try {playMode(mode);}
                catch (RemoteException e) {e.printStackTrace();}
            }
        };registerReceiver(receiver1,filter1);

        IntentFilter filter2 = new IntentFilter("PLAYBACK");
        receiver2 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String mode = "PLAYBACK";
                try {playMode(mode);}
                catch (RemoteException e) {e.printStackTrace();}
            }
        };registerReceiver(receiver2,filter2);

        IntentFilter filter3 = new IntentFilter("PLAYSTOP");
        receiver3 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String mode = "PLAYNEXT";
                try {playMode(mode);}
                catch (RemoteException e) {e.printStackTrace();}
            }
        };registerReceiver(receiver3,filter3);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(player != null)
            player.release();
        unregisterReceiver(receiver1);
        unregisterReceiver(receiver2);
        unregisterReceiver(receiver3);
        nManager.cancel(0);
    }

    IMyAidl.Stub mBinder = new IMyAidl.Stub() {
        @Override
        public void sendMessage(String mes) throws RemoteException {
            //pause&back
            try {playMode(mes);}
            catch (RemoteException e) {e.printStackTrace();}
        }

        //재생중인지 액티비티에게전달
        @Override
        public boolean getIsPlaying() throws RemoteException {
            boolean flag = false;
            if (player.isPlaying())
                flag = true;
            if(wasStart)
                aidlBack.fileName_fromSerice(mpList[idx].substring(0,mpList[idx].length()-4));          //액티비티에거 파일명 전송
            return flag  ;
        }

        //aidlBack 등록
        @Override
        public void registerAidlBack(IMyAidlBack aidl) throws RemoteException {
            aidlBack = aidl;
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //start&stop
        try {playMode(intent.getStringExtra("MODE"));}
        catch (RemoteException e) {e.printStackTrace();}
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        getMp3();
        prepareMP3();
        notiSet();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    //0.5초마다 prgressbar 갱신
    Handler handler = new Handler(){
        public void handleMessage(Message msg){
            if(player == null)
                return;
            if(player.isPlaying()) {
                rView.setProgressBar(R.id.NprogressBar, player.getDuration(), player.getCurrentPosition(), false);
                nManager.notify(0, builder.build());
            }
            handler.sendEmptyMessageDelayed(0,500);
        }
    };

    /////////////////////////////////////////////////
    public void playMode(String mode) throws RemoteException {
        //Log.d("super","playmode (service) : "+mode);
        try {
            switch (mode) {
                case "PLAYSTART": {
                    player.start();
                    wasStart = true;
                    aidlBack.fileName_fromSerice(mpList[idx].substring(0,mpList[idx].length()-4));          //액티비티에거 파일명 전송
                    rView.setTextViewText(R.id.NtxtName, mpList[idx].substring(0,mpList[idx].length()-4));  //noti 파일명 설정
                    rView.setImageViewResource(R.id.Nimgv,R.drawable.im);//noti 이미지 설정
                    rView.setTextViewText(R.id.Nbtn2,"||");         //noti 버튼변경
                    builder.setSmallIcon(R.drawable.play);              //noti icon설정
                    break;
                }
                case "PLAYPAUSE": {
                    player.pause();
                    rView.setTextViewText(R.id.Nbtn2,"▶");         //noti 버튼변경
                    builder.setSmallIcon(R.drawable.pause);              //noti icon설정
                    break;
                }
                case "PLAYBACK": {
                    idx--;
                    player.reset();
                    prepareMP3();
                    player.start();
                    break;
                }
                case "PLAYNEXT":{
                    idx++;
                    player.reset();
                    prepareMP3();
                    player.start();
                    break;
                }
            }
            nManager.notify(0,builder.build());
        }catch (Exception e){Log.d("super","play error "+mode+"  "+e.toString());}
    }

    public void getMp3(){
        File dir = new File(sdpath);
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mp3");
            }
        };
        mpList = dir.list(filter);
    }
    public void prepareMP3()  {
        String path = sdpath + "/" + mpList[idx];
        try{
            player.setDataSource(path);
            player.prepare();
        }
        catch (Exception e){
            Log.d("super","prepare(service) error : " + e.getMessage());
        }
    }

    /////--------------------   Listener   --------------------///
    MediaPlayer.OnCompletionListener onComplete = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            try {
                idx = (idx == mpList.length-1 ? 0: idx+1);
                player.reset();
                prepareMP3();
                player.start();

                aidlBack.fileName_fromSerice(mpList[idx]);

                rView.setProgressBar(R.id.NprogressBar, player.getDuration(), player.getCurrentPosition(), false);
                rView.setTextViewText(R.id.NtxtName, mpList[idx].substring(0,mpList[idx].length()-4));
                nManager.notify(0,builder.build());
            } catch (Exception e) {
                Log.d("super","listener(service) error : " + e.getMessage());
            }
        }
    };

//////-------------------   noti   ----------------------------/////

    NotificationManager nManager = null;
    NotificationChannel nChannel = null;
    NotificationCompat.Builder builder = null;
    RemoteViews rView = null;


    public void notiSet(){

        Intent activityIntent = new Intent(this,MainActivity.class);
        Intent broadIntent1 = new Intent("PLAYBACK");
        Intent broadIntent2 = new Intent("PLAYSTART");
        Intent broadIntent3 = new Intent("PLAYNEXT");

        rView = new RemoteViews(getPackageName(), R.layout.activity_noti);
        rView.setOnClickPendingIntent(R.id.Nimgv,PendingIntent.getActivity(this,0,activityIntent,0));
        rView.setOnClickPendingIntent(R.id.Nbtn1,PendingIntent.getBroadcast(this, 0,broadIntent1,0));
        rView.setOnClickPendingIntent(R.id.Nbtn2,PendingIntent.getBroadcast(this, 0,broadIntent2,0));
        rView.setOnClickPendingIntent(R.id.Nbtn3,PendingIntent.getBroadcast(this, 0,broadIntent3,0));
        rView.setProgressBar(R.id.NprogressBar,100,0,false);

        builder = new NotificationCompat.Builder(this)
                .setOngoing(true)
                .setCustomBigContentView(rView)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false);

        nManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            nChannel = new NotificationChannel("id","name",NotificationManager.IMPORTANCE_DEFAULT);
            builder.setChannelId("id");
            nManager.createNotificationChannel(nChannel);
        }

        Notification noti = new Notification();
        startForeground(1234,noti);
    }

}
