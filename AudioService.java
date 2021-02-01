
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.dzensport.androidclient.DTO.GetParsableLogicForTimer;
import com.dzensport.androidclient.DTO.TabataDTO;
import com.dzensport.androidclient.DTO.TabataEventLogicDTO;
import com.dzensport.androidclient.MainActivity;
import com.dzensport.androidclient.R;
import com.dzensport.androidclient.sharedprefs.TabataStateSharedPreferencesDTO;
import com.dzensport.androidclient.tabata.AdapterTabataSP;

import java.util.Timer;
import java.util.TimerTask;

public class AudioService extends Service {
    private Handler timerHandler;
    private Runnable timerRunnable;

    private final IBinder mBinder = new LocalService();

    //logic data
    SharedPreferences tabatastateFile;
    private SharedPreferences.Editor editor = null;
    TabataDTO tabataDTO;
    TabataEventLogicDTO tabataEventLogicDTO;
    AdapterTabataSP adapterTabataSP;
    private int id = 1;
    boolean firstStartDone = false; //+
    float volume = 1.0f; //+
    //Cursors of time nav
    long pauseValue = 0;  //+
    int timerCursor = 0; //+
    boolean isTimerStart = false; // Play | Pause timer +
    long startTabataChronometerStringLong = 0;
    long tabataStartAdditionalTotalChronometerStringLong = 0;
    long startTabataChronometerStringLongFromService = 0;

    //service values
    long timePassedAfterStartCycle = 0;



    //new
    private Timer timer = new Timer();
    public MediaPlayer gongStart;
    public MediaPlayer gongStop;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public class LocalService extends Binder {
        AudioService getService(){
            return AudioService.this;
        }
    }



    @Override
    public void onCreate() {

        gongStart = MediaPlayer.create(this, R.raw.gong);
        gongStop = MediaPlayer.create(this, R.raw.gongend);

        getFromSharedPrefs(); //read data


        adapterTabataSP = new AdapterTabataSP();
        tabataDTO = adapterTabataSP.getTabataFromSP(getApplicationContext(), id);


        //parse DTO to logic object of timer cycles
        tabataEventLogicDTO = GetParsableLogicForTimer.getParsableLogicForTimer(tabataDTO);

     //   final MediaPlayer gongStart = MediaPlayer.create(this, R.raw.gong);
     //   final MediaPlayer gongStop = MediaPlayer.create(this, R.raw.gongend);
        gongStart.setVolume(volume, volume); //если нажали кнопку mute
        gongStop.setVolume(volume, volume); //если нажали кнопку mute

        timePassedAfterStartCycle = SystemClock.elapsedRealtime() - startTabataChronometerStringLong;





        super.onCreate();




    }





    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //leave the function here. Called every time the fragment ends (onCreate() method is called once !!!!!)
        getFromSharedPrefs();
        createNotification(); //notification for background work
        startTimer(); //new

        System.out.println("onStartCommand");

        if (intent != null) { //V1.06 fix
            if (intent.getExtras().getBoolean("stop")) {
                System.out.println("onStartCommand INTENT = stop");
                stopForeground(true);
                stopSelf();
                saveToPrefs(); /////ТЕСТ!!!!!!!!!!!!!!!!!!
            }
        }

        //return START_STICKY; //Error V1.06
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimer(); //stop timer class
    }


    public void saveToPrefs() {
        tabatastateFile = getSharedPreferences(TabataStateSharedPreferencesDTO.TABATASTATELOCALFILENAME, Context.MODE_PRIVATE);
        editor = tabatastateFile.edit();
        firstStartDone = true;
        //Save arguments to shared prefs from intent
        editor.putBoolean(TabataStateSharedPreferencesDTO.FIRSTSTARTDONE, firstStartDone);
        editor.putLong(TabataStateSharedPreferencesDTO.PAUSEVALUE, pauseValue);
        editor.putInt(TabataStateSharedPreferencesDTO.TIMERCURSOR, timerCursor);
        editor.putBoolean(TabataStateSharedPreferencesDTO.ISTIMERSTART, isTimerStart);
        editor.putLong(TabataStateSharedPreferencesDTO.STARTTABATACHRONOMETERSTRINGLONG, SystemClock.elapsedRealtime() - timePassedAfterStartCycle); //рабочее


        editor.putLong(TabataStateSharedPreferencesDTO.SERVICECURSOR, timePassedAfterStartCycle);

        editor.commit();
    }


    public void getFromSharedPrefs() {
        tabatastateFile = getSharedPreferences(TabataStateSharedPreferencesDTO.TABATASTATELOCALFILENAME, Context.MODE_PRIVATE);
        id = tabatastateFile.getInt(TabataStateSharedPreferencesDTO.IDOFSTARTTABATA, 1);
        firstStartDone = tabatastateFile.getBoolean(TabataStateSharedPreferencesDTO.FIRSTSTARTDONE, false);
        volume = tabatastateFile.getFloat(TabataStateSharedPreferencesDTO.VOLUME, 1.0f);
        pauseValue = tabatastateFile.getLong(TabataStateSharedPreferencesDTO.PAUSEVALUE, 0);
        timerCursor = tabatastateFile.getInt(TabataStateSharedPreferencesDTO.TIMERCURSOR, 0);
        isTimerStart = tabatastateFile.getBoolean(TabataStateSharedPreferencesDTO.ISTIMERSTART, true);
        startTabataChronometerStringLong = tabatastateFile.getLong(TabataStateSharedPreferencesDTO.STARTTABATACHRONOMETERSTRINGLONG, 0);
        tabataStartAdditionalTotalChronometerStringLong = tabatastateFile.getLong(TabataStateSharedPreferencesDTO.TABATASTARTADDITIONALTOTALCHRONOMETERSTRINGLONG, 0);

        timePassedAfterStartCycle = SystemClock.elapsedRealtime() - startTabataChronometerStringLong;
    }



    public void createNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Tabata",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);



            //Click link to notification
            Intent notifyIntent = new Intent(this, MainActivity.class);
            // Set the Activity to start in a new, empty task
            notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // Create the PendingIntent
            PendingIntent notifyPendingIntent = PendingIntent.getActivity(
                    this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
            );



            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_baseline_timer_white_24)
                    .setContentTitle(tabataDTO.getName() + " " + getString(R.string.tabataNotificationLaunchWorking))
                    .setContentText(getString(R.string.tabataNotificationLaunch))
                    .setContentIntent(notifyPendingIntent)
                    .build();

            startForeground(1, notification);

        }
    }

    public void startTimer() {
        timer.scheduleAtFixedRate(new TimerLogicClass(), 0, 1000);
    }

    public void stopTimer() {
        timer.cancel();
    }






    private class TimerLogicClass extends TimerTask {


        @Override
        public void run() {


            if (isTimerStart) { //start timer (0.5 sec step)

                saveToPrefs();
                timePassedAfterStartCycle += 1000;

                if(timePassedAfterStartCycle >= (tabataEventLogicDTO.getTabataEventCycleList().get(timerCursor).getTimeInSec() * 1000)) {
                    //Play sound from DTO logic
                    switch (tabataEventLogicDTO.getTabataEventCycleList().get(timerCursor).getStatusOfWorkType()) {
                        case 1: //play - REST
                            gongStop.start();
                            break;
                        case 2: //pause - STARTING
                            gongStart.start();
                            break;
                        case 3: //pause between exercises - STARTING
                            gongStart.start();
                            break;
                        default:
                            break;
                    }
                    //logic -> next event cycle
                    if (timerCursor < tabataEventLogicDTO.getTabataEventCycleList().size()-1) {
                        timePassedAfterStartCycle = 0;
                        timerCursor++;
                        startTabataChronometerStringLongFromService = SystemClock.elapsedRealtime(); //pass to timer fragment (time tart cycle)
                    } else {
                        //do nothing
                        isTimerStart = false; //pause (break timer)
                    }

                }
            }

            else {
                //pause
                getFromSharedPrefs();
                isTimerStart = false;
            }
        }
    }

}
